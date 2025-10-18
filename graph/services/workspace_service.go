package services

import (
	"context"
	"errors"
	"fmt"
	"graph/models"
	"log"
	"strings"
	"time"

	"github.com/yorkie-team/yorkie/admin"
	"github.com/yorkie-team/yorkie/api/types"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type WorkspaceService struct {
	db           *mongo.Database
	graphService *GraphService
	yorkieAdmin  *admin.Client
}

func NewWorkspaceService(db *mongo.Database, gs *GraphService, yorkieAdmin *admin.Client) *WorkspaceService {
	return &WorkspaceService{db: db, graphService: gs, yorkieAdmin: yorkieAdmin}
}

func (s *WorkspaceService) CreateWorkspace(ctx context.Context, title, wsType, creatorID string) (string, error) {
	if title == "" || wsType == "" || creatorID == "" {
		return "", errors.New("title, type, creatorID is nil")
	}

	wsType = strings.ToLower(strings.TrimSpace(wsType))
	if !isValidWorkspaceType(wsType) {
		return "", fmt.Errorf("invalid workspace type: %s", wsType)
	}

	projectName := fmt.Sprintf("workspace-%s-%s", creatorID, title)
	project, err := s.yorkieAdmin.CreateProject(ctx, projectName)
	if err != nil {
		return "", fmt.Errorf("failed to create yorkie project: %w", err)
	}

	doc := bson.M{
		"title":             title,
		"type":              wsType,
		"creator_id":        creatorID,
		"yorkie_project_id": project.ID.String(),
		"yorkie_public_key": project.PublicKey,
		"yorkie_secret_key": project.SecretKey,
		"created_at":        time.Now(),
		"updated_at":        time.Now(),
	}

	res, err := s.db.Collection("workspaces").InsertOne(ctx, doc)
	if err != nil {
		return "", fmt.Errorf("filaed to create workspace: %w", err)
	}

	authWebhookURL := "http://user-service:8080/api/v1/yorkie/auth"
	authWebhookMethods := []string{"AttachDocument", "PushPull", "WatchDocuments"}
	_, err = s.yorkieAdmin.UpdateProject(ctx, project.ID.String(), &types.UpdatableProjectFields{
		AuthWebhookURL:     &authWebhookURL,
		AuthWebhookMethods: &authWebhookMethods,
	})
	if err != nil {
		log.Printf("failed to set auth webhook: %v", err)
	}

	return res.InsertedID.(primitive.ObjectID).Hex(), nil
}

func (s *WorkspaceService) CheckWorkspace(ctx context.Context, workspaceID, userID string) (string, bool, error) {
	objID, err := primitive.ObjectIDFromHex(workspaceID)
	if err != nil {
		return "", false, fmt.Errorf("invalid workspaceID: %w", err)
	}

	var ws struct {
		Type      string `bson:"type"`
		CreatorID string `bson:"creator_id"`
	}
	err = s.db.Collection("workspaces").FindOne(ctx, bson.M{"_id": objID, "creator_id": userID}).Decode(&ws)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return "", false, nil
		}
		return "", false, err
	}
	return ws.Type, true, nil
}

func (s *WorkspaceService) UpdateWorkspace(ctx context.Context, workspaceID, userID string, title, wsType *string) (string, error) {
	objID, err := primitive.ObjectIDFromHex(workspaceID)
	if err != nil {
		return "", fmt.Errorf("invalid workspaceID: %w", err)
	}

	setMap := bson.M{"updated_at": time.Now()}
	if title != nil {
		setMap["title"] = *title
	}
	if wsType != nil {
		t := strings.ToLower(strings.TrimSpace(*wsType))
		if !isValidWorkspaceType(t) {
			return "", fmt.Errorf("unsupported workspace type: %s", t)
		}
		setMap["type"] = t
	}

	res, err := s.db.Collection("workspaces").UpdateOne(ctx,
		bson.M{"_id": objID, "creator_id": userID},
		bson.M{"$set": setMap},
	)
	if err != nil {
		return "", err
	}
	if res.MatchedCount == 0 {
		return "", errors.New("workspace not found or unauthorized")
	}
	msg := "workspace updated successfully"
	if res.ModifiedCount == 0 {
		msg = "no changes applied"
	}
	return msg, nil
}

func (s *WorkspaceService) DeleteWorkspace(ctx context.Context, workspaceID, userID string) error {
	objID, err := primitive.ObjectIDFromHex(workspaceID)
	if err != nil {
		return fmt.Errorf("invalid workspaceID: %w", err)
	}

	// 워크스페이스, 연결 삭제
	if _, err := s.db.Collection("connections").DeleteMany(ctx, bson.M{"workspace_id": workspaceID}); err != nil {
		return err
	}
	res, err := s.db.Collection("workspaces").DeleteOne(ctx, bson.M{"_id": objID, "creator_id": userID})
	// todo. 노트도 삭제해야해요 ㅜㅜ
	if err != nil {
		return err
	}
	if res.DeletedCount == 0 {
		return errors.New("workspace not found or unauthorized")
	}
	return nil
}

// goroutine
func (s *WorkspaceService) ChangeWorkspaceStyle(ctx context.Context, workspaceID string, userID string, newStyle string) (string, error) {
	if strings.TrimSpace(workspaceID) == "" {
		return "", errors.New("workspaceID is blank")
	}
	if strings.TrimSpace(newStyle) == "" {
		return "", errors.New("newStyle is blank")
	}
	newStyle = strings.ToLower(strings.TrimSpace(newStyle))
	if !isValidWorkspaceType(newStyle) {
		return "", errors.New("invalid workspace type. choose one styles (such as 'zettel', 'generic', 'para')")
	}

	if s.graphService == nil {
		return "", errors.New("graphService is nil")
	}

	var currentTitle *string
	_, err := s.UpdateWorkspace(ctx, workspaceID, userID, currentTitle, &newStyle)
	if err != nil {
		return "", fmt.Errorf("failed to update workspace type: %w", err)
	}

	jobID := primitive.NewObjectID()
	job := bson.M{
		"_id":          jobID,
		"workspace_id": workspaceID,
		"type":         newStyle,
		"status":       "pending",
		"created_at":   time.Now(),
		"updated_at":   time.Now(),
	}
	_, err = s.db.Collection("workspace_jobs").InsertOne(ctx, job)
	if err != nil {
		return "", fmt.Errorf("failed to create queueing history: %w", err)
	}

	go func(jobID primitive.ObjectID, workspaceID, style string) {
		ctxBG := context.Background()

		var err error
		switch style {
		case models.WorkspaceTypeZettel:
			_, err = s.graphService.AutoConnectWorkspace(ctxBG, workspaceID)
		case models.WorkspaceTypeGeneric, models.WorkspaceTypePara:
			_, err = s.graphService.ClearPendingConnections(ctxBG, workspaceID)
		}

		status := "success"
		if err != nil {
			status = "failed"
		}

		_, _ = s.db.Collection("workspace_jobs").UpdateOne(
			ctxBG,
			bson.M{"_id": jobID},
			bson.M{"$set": bson.M{
				"status":     status,
				"updated_at": time.Now(),
			}},
		)
	}(jobID, workspaceID, newStyle)

	return fmt.Sprintf("changed to workspace type: '%s'. (async works: %s)", newStyle, jobID.Hex()), nil
}

func isValidWorkspaceType(t string) bool {
	switch strings.ToLower(strings.TrimSpace(t)) {
	case models.WorkspaceTypeZettel, models.WorkspaceTypeGeneric, models.WorkspaceTypePara:
		return true
	default:
		return false
	}
}
