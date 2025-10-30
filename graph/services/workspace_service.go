package services

import (
	"context"
	"errors"
	"fmt"
	"graph/models"
	"log"
	"regexp"
	"strings"
	"time"

	"github.com/mozillazg/go-unidecode"
	"github.com/yorkie-team/yorkie/admin"
	"github.com/yorkie-team/yorkie/api/types"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type WorkspaceService struct {
	db             *mongo.Database
	graphService   *GraphService
	yorkieAdmin    *admin.Client
	authWebhookURL string
}

var nonSlugChar = regexp.MustCompile(`[^a-z0-9-]+`)

func generateProjectName(userID, title string) string {
	const maxProjectNameLen = 30

	userIDRunes := []rune(userID)
	maxUserIDLen := maxProjectNameLen / 2
	if len(userIDRunes) > maxUserIDLen {
		userIDRunes = userIDRunes[:maxUserIDLen]
	}
	userIDPart := string(userIDRunes)

	maxSlugLen := maxProjectNameLen - len([]rune(userIDPart)) - 1
	if maxSlugLen < 5 {
		maxSlugLen = 5
	}

	slug := toSlug(title, maxSlugLen)
	return fmt.Sprintf("%s-%s", userIDPart, slug)
}

func toSlug(s string, maxLen int) string {
	s = strings.ToLower(s)
	s = unidecode.Unidecode(s)
	s = nonSlugChar.ReplaceAllString(s, "-")
	for strings.Contains(s, "--") {
		s = strings.ReplaceAll(s, "--", "-")
	}
	s = strings.Trim(s, "-")
	runes := []rune(s)
	if maxLen > 0 && len(runes) > maxLen {
		s = string(runes[:maxLen])
		s = strings.TrimRight(s, "-")
	}
	return s
}

func NewWorkspaceService(db *mongo.Database, gs *GraphService, yorkieAdmin *admin.Client, authWebhookURL string) *WorkspaceService {
	return &WorkspaceService{
		db:             db,
		graphService:   gs,
		yorkieAdmin:    yorkieAdmin,
		authWebhookURL: authWebhookURL,
	}
}

func (s *WorkspaceService) CreateWorkspace(ctx context.Context, title, wsType, userID string) (string, error) {
	if title == "" || wsType == "" || userID == "" {
		return "", errors.New("title, type, userID is nil")
	}

	wsType = strings.ToLower(strings.TrimSpace(wsType))
	if !isValidWorkspaceType(wsType) {
		return "", fmt.Errorf("invalid workspace type: %s", wsType)
	}

	if s.yorkieAdmin == nil {
		return "", errors.New("yorkieAdmin client is not initialized")
	}

	if title == "" {
		return "", errors.New("workspace title is too generic or empty after slug transformation")
	}

	projectName := generateProjectName(userID, title)
	project, err := s.yorkieAdmin.CreateProject(ctx, projectName)
	if err != nil {
		return "", fmt.Errorf("failed to create yorkie project: %w", err)
	}

	doc := models.Workspace{
		Title:           title,
		Type:            wsType,
		UserID:          userID,
		YorkieProjectID: project.ID.String(),
		YorkiePublicKey: project.PublicKey,
		YorkieSecretKey: project.SecretKey,
		CreatedAt:       time.Now(),
		UpdatedAt:       time.Now(),
	}

	res, err := s.db.Collection("workspaces").InsertOne(ctx, doc)
	if err != nil {
		return "", fmt.Errorf("filaed to create workspace: %w", err)
	}

	authWebhookURL := s.authWebhookURL
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

	var ws models.Workspace
	err = s.db.Collection("workspaces").FindOne(ctx, bson.M{"_id": objID, "user_id": userID}).Decode(&ws)
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

	var currentWs models.Workspace
	err = s.db.Collection("workspaces").FindOne(ctx, bson.M{"_id": objID, "user_id": userID}).Decode(&currentWs)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return "", errors.New("workspace not found or unauthorized")
		}
		return "", fmt.Errorf("failed to fetch workspace: %w", err)
	}

	setMap := bson.M{"updated_at": time.Now()}

	if title != nil && currentWs.Title != *title {
		setMap["title"] = *title

		if s.yorkieAdmin != nil {
			newProjectName := generateProjectName(currentWs.UserID, *title)

			_, err = s.yorkieAdmin.UpdateProject(ctx, currentWs.YorkieProjectID, &types.UpdatableProjectFields{
				Name: &newProjectName,
			})
			if err != nil {
				log.Printf("failed to update yorkie project name for %s: %v", currentWs.YorkieProjectID, err)
			}
		} else {
			log.Println("yorkieAdmin client is nil, cannot update project name")
		}
	}

	if wsType != nil {
		t := strings.ToLower(strings.TrimSpace(*wsType))
		if !isValidWorkspaceType(t) {
			return "", fmt.Errorf("unsupported workspace type: %s", t)
		}
		setMap["type"] = t
	}

	res, err := s.db.Collection("workspaces").UpdateOne(ctx,
		bson.M{"_id": objID, "user_id": userID},
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
	res, err := s.db.Collection("workspaces").DeleteOne(ctx, bson.M{"_id": objID, "user_id": userID})
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
		log.Printf("[ChangeStyle] Error: Failed to update workspace type in DB: %v", err)
		return "", fmt.Errorf("failed to update workspace type: %w", err)
	}
	log.Printf("[ChangeStyle] Success: Workspace DB type updated to '%s'", newStyle)

	jobID := primitive.NewObjectID()
	job := models.WorkspaceJob{
		ID:          jobID,
		WorkspaceID: workspaceID,
		Type:        newStyle,
		Status:      "pending",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	log.Printf("[ChangeStyle] Queueing Job ID: %s, Status: pending", jobID.Hex())

	_, err = s.db.Collection("workspace_jobs").InsertOne(ctx, job)
	if err != nil {
		return "", fmt.Errorf("failed to create queueing history: %w", err)
	}

	log.Printf("[ChangeStyle] Starting async graph processing for Job ID: %s, Style: %s", jobID.Hex(), newStyle)
	go func(jobID primitive.ObjectID, workspaceID, style string) {
		ctxBG := context.Background()

		var err error
		switch style {
		case models.WorkspaceTypeZettel:
			log.Printf("[Job %s] Starting AutoConnectWorkspace for Zettel style.", jobID.Hex())
			_, err = s.graphService.AutoConnectWorkspace(ctxBG, workspaceID)
			if err != nil {
				log.Printf("[Job %s] Error: AutoConnectWorkspace failed: %v", jobID.Hex(), err)
			} else {
				log.Printf("[Job %s] Success: AutoConnectWorkspace completed.", jobID.Hex())
			}
		case models.WorkspaceTypeGeneric, models.WorkspaceTypePara:
			log.Printf("[Job %s] Starting ClearPendingConnections for %s style.", jobID.Hex(), style)
			_, err = s.graphService.ClearPendingConnections(ctxBG, workspaceID)
			if err != nil {
				log.Printf("[Job %s] Error: ClearPendingConnections failed: %v", jobID.Hex(), err)
			} else {
				log.Printf("[Job %s] Success: ClearPendingConnections completed.", jobID.Hex())
			}
		}

		status := "success"
		if err != nil {
			status = "failed"
		}

		_, updateErr := s.db.Collection("workspace_jobs").UpdateOne(
			ctxBG,
			bson.M{"_id": jobID},
			bson.M{"$set": bson.M{
				"status":     status,
				"updated_at": time.Now(),
			}},
		)

		if updateErr != nil {
			log.Printf("[Job %s] Fatal Error: Failed to update job status to '%s' in DB: %v", jobID.Hex(), status, updateErr)
		} else {
			log.Printf("[Job %s] Success: Job status updated to '%s'.", jobID.Hex(), status)
		}
	}(jobID, workspaceID, newStyle)

	return fmt.Sprintf("changed to workspace type: '%s'. (async works: %s)", newStyle, jobID.Hex()), nil
}

func (s *WorkspaceService) FindAllWorkspacesByUserID(ctx context.Context, userID string) ([]*models.Workspace, error) {
	if userID == "" {
		return nil, errors.New("userID is blank")
	}

	filter := bson.M{"user_id": userID}
	cursor, err := s.db.Collection("workspaces").Find(ctx, filter)
	if err != nil {
		return nil, fmt.Errorf("failed to find workspaces: %w", err)
	}
	defer cursor.Close(ctx)

	var workspaces []*models.Workspace
	for cursor.Next(ctx) {
		var ws models.Workspace
		if err := cursor.Decode(&ws); err != nil {
			return nil, fmt.Errorf("failed to decode workspace document: %w", err)
		}
		workspaces = append(workspaces, &ws)
	}

	if err := cursor.Err(); err != nil {
		return nil, fmt.Errorf("cursor iteration error: %w", err)
	}

	return workspaces, nil
}

func isValidWorkspaceType(t string) bool {
	switch strings.ToLower(strings.TrimSpace(t)) {
	case models.WorkspaceTypeZettel, models.WorkspaceTypeGeneric, models.WorkspaceTypePara:
		return true
	default:
		return false
	}
}
