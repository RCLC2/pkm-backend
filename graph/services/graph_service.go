package services

import (
	"bytes"
	"context"
	"encoding/json"
	"log"

	"fmt"
	"graph/models"
	"io"
	"net/http"
	"net/url"
	"os"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const (
	StatusPending   = "pending"
	StatusConfirmed = "confirmed"
)

var topicUrl = os.Getenv("TOPIC_SERVICE_URL")
var noteUrl = os.Getenv("NOTE_SERVICE_URL")

type GraphService struct {
	db                    *mongo.Database
	FetchSimilarByID      func(ctx context.Context, docID string, topN int) ([]string, error)
	FetchSimilarByContent func(ctx context.Context, content string, topN int) ([]string, error)
	FetchDocumentIDs      func(ctx context.Context, workspaceID string) ([]string, error)
}

func NewGraphService(db *mongo.Database) *GraphService {
	return &GraphService{
		db:                    db,
		FetchSimilarByID:      fetchSimilarDocsByIDHTTP,
		FetchSimilarByContent: fetchSimilarDocsByContentHTTP,
		FetchDocumentIDs:      fetchDocumentIDsHTTP,
	}
}

func fetchDocumentIDsHTTP(ctx context.Context, workspaceID string) ([]string, error) {
	apiURL := fmt.Sprintf("%s/note/ids", noteUrl)

	reqURL, err := url.Parse(apiURL)
	if err != nil {
		return nil, fmt.Errorf("invalid api url: %w", err)
	}
	q := reqURL.Query()
	q.Set("workspaceId", workspaceID)
	reqURL.RawQuery = q.Encode()

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, reqURL.String(), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create document IDs request: %w", err)
	}

	client := &http.Client{Timeout: 3 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to call document service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("document service returned non-200 status: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read document service response body: %w", err)
	}

	var result struct {
		IDs []string `json:"ids"`
	}
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("failed to parse document service response: %w", err)
	}

	return result.IDs, nil
}

func fetchSimilarDocsByContentHTTP(ctx context.Context, content string, topN int) ([]string, error) {
	reqBody, err := json.Marshal(map[string]interface{}{
		"content": content,
		"topN":    topN,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to marshal content request body: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, topicUrl+"/find-similar/by-content", bytes.NewReader(reqBody))

	if err != nil {
		return nil, fmt.Errorf("failed to create content request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	return doTopicServiceRequest(ctx, req)
}

func fetchSimilarDocsByIDHTTP(ctx context.Context, docID string, topN int) ([]string, error) {
	u, err := url.Parse(topicUrl + "/find-similar/by-id")
	if err != nil {
		return nil, fmt.Errorf("failed to parse topic service URL: %w", err)
	}
	q := u.Query()
	q.Set("noteId", docID)
	u.RawQuery = q.Encode()

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create ID request: %w", err)
	}

	return doTopicServiceRequest(ctx, req)
}

func doTopicServiceRequest(ctx context.Context, req *http.Request) ([]string, error) {
	client := &http.Client{Timeout: 3 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to call topic service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("topic service returned non-200 status: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var result struct {
		IDs []string `json:"ids"`
	}
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("failed to parse topic service response: %w", err)
	}

	return result.IDs, nil
}

func objectIDFromHex(idStr string, fieldName string) (primitive.ObjectID, error) {
	objID, err := primitive.ObjectIDFromHex(idStr)
	if err != nil {
		return primitive.NilObjectID, fmt.Errorf("invalid %s ID: %w", fieldName, err)
	}
	return objID, nil
}

func makeConnection(sourceID, targetID primitive.ObjectID, workspaceID, status string) models.GraphConnection {
	now := time.Now()
	return models.GraphConnection{
		SourceID:    sourceID,
		TargetID:    targetID,
		Status:      status,
		WorkspaceID: workspaceID,
		CreatedAt:   now,
		UpdatedAt:   now,
	}
}

func (s *GraphService) NoteCreated(ctx context.Context, newDocID string, workspaceID string) ([]models.GraphConnection, error) {
	sourceID, err := objectIDFromHex(newDocID, "new document")
	if err != nil {
		return nil, err
	}

	similarIDs, err := s.FetchSimilarByID(ctx, newDocID, 5)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch similar docs by ID: %w", err)
	}

	var connections []models.GraphConnection
	connColl := s.db.Collection("graph_connections")

	for _, targetIDStr := range similarIDs {
		if newDocID == targetIDStr {
			continue
		}

		targetID, err := objectIDFromHex(targetIDStr, "target document")
		if err != nil {
			continue
		}

		// 정방향
		conn := makeConnection(sourceID, targetID, workspaceID, StatusPending)
		if _, err := connColl.InsertOne(ctx, conn); err == nil {
			connections = append(connections, conn)
		}

		// 역방향
		// revConn := makeConnection(targetID, sourceID, workspaceID, StatusPending)
		// _, _ = connColl.InsertOne(ctx, revConn) // 실패 무시
	}

	return connections, nil
}

func (s *GraphService) NoteDeleted(ctx context.Context, docID string, workspaceID string) error {
	objID, err := objectIDFromHex(docID, "document")
	if err != nil {
		return err
	}

	_, err = s.db.Collection("graph_connections").DeleteMany(
		ctx,
		bson.M{
			"workspace_id": workspaceID,
			"$or": []bson.M{
				{"source_id": objID},
				{"target_id": objID},
			}},
	)
	if err != nil {
		return fmt.Errorf("failed to delete related connections: %w", err)
	}
	return nil
}

func (s *GraphService) ConfirmGraphConnection(ctx context.Context, sourceID, targetID string, workspaceID string) error {
	sourceObjID, err := primitive.ObjectIDFromHex(sourceID)
	if err != nil {
		return fmt.Errorf("invalid source ID: %w", err)
	}
	targetObjID, err := primitive.ObjectIDFromHex(targetID)
	if err != nil {
		return fmt.Errorf("invalid target ID: %w", err)
	}

	_, err = s.db.Collection("graph_connections").UpdateOne(
		ctx,
		bson.M{"source_id": sourceObjID, "target_id": targetObjID, "workspace_id": workspaceID},
		bson.M{"$set": bson.M{"status": StatusConfirmed, "updated_at": time.Now()}},
	)
	return err
}

func (s *GraphService) EditGraphConnection(ctx context.Context, sourceID, targetID string, workspaceID string) error {
	sourceObjID, err := primitive.ObjectIDFromHex(sourceID)
	if err != nil {
		return fmt.Errorf("invalid source ID: %w", err)
	}
	targetObjID, err := primitive.ObjectIDFromHex(targetID)
	if err != nil {
		return fmt.Errorf("invalid target ID: %w", err)
	}

	newConn := makeConnection(sourceObjID, targetObjID, workspaceID, StatusPending)
	opts := options.Update().SetUpsert(true)
	filter := bson.M{"source_id": sourceObjID, "target_id": targetObjID, "workspace_id": workspaceID}
	update := bson.M{"$set": newConn}

	_, err = s.db.Collection("graph_connections").UpdateOne(
		ctx,
		filter,
		update,
		opts,
	)
	return err
}

func (s *GraphService) ConfirmAllConnections(ctx context.Context, workspaceID string) error {
	_, err := s.db.Collection("graph_connections").UpdateMany(
		ctx,
		bson.M{"workspace_id": workspaceID, "status": StatusPending},
		bson.M{"$set": bson.M{"status": StatusConfirmed, "updated_at": time.Now()}},
	)
	return err
}

func (s *GraphService) AutoConnectWorkspace(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	log.Printf("[AutoConnect] Started for Workspace ID: %s", workspaceID)

	docIDs, err := s.FetchDocumentIDs(ctx, workspaceID)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch workspace doc IDs: %w", err)
	}
	log.Printf("[AutoConnect] Fetched %d document IDs.", len(docIDs))

	var connections []models.GraphConnection
	connColl := s.db.Collection("graph_connections")

	for i, docID := range docIDs {
		log.Printf("[AutoConnect %s/%d] Processing document ID: %s", workspaceID, i+1, docID)
		sourceID, err := objectIDFromHex(docID, "source document")
		if err != nil {
			log.Printf("[AutoConnect %s] Warning: Invalid document ID %s, skipping.", workspaceID, docID)
			continue
		}

		similarIDs, err := s.FetchSimilarByID(ctx, docID, 5)
		if err != nil {
			log.Printf("[AutoConnect %s] Warning: Failed to fetch similar docs for %s: %v, skipping.", workspaceID, docID, err)
			continue
		}

		log.Printf("[AutoConnect %s] Found %d similar IDs for %s.", workspaceID, len(similarIDs), docID)
		for _, targetIDStr := range similarIDs {
			if docID == targetIDStr {
				continue
			}

			targetID, err := objectIDFromHex(targetIDStr, "target document")
			if err != nil {
				log.Printf("[AutoConnect %s] Warning: Invalid target ID %s, skipping connection.", workspaceID, targetIDStr)
				continue
			}

			conn := makeConnection(sourceID, targetID, workspaceID, StatusPending)
			_, _ = connColl.UpdateOne(
				ctx,
				bson.M{"source_id": sourceID, "target_id": targetID},
				bson.M{"$set": conn},
				options.Update().SetUpsert(true),
			)
			connections = append(connections, conn)

			// todo 양방향
			/*
				revConn := makeConnection(targetID, sourceID, workspaceID, StatusPending)
				_, _ = connColl.UpdateOne(
					ctx,
					bson.M{"source_id": targetID, "target_id": sourceID},
					bson.M{"$set": revConn},
					options.Update().SetUpsert(true),
				)
			*/
		}
	}

	log.Printf("[AutoConnect] Finished. Total %d connections processed for WS ID: %s", len(connections), workspaceID)
	return connections, nil
}

func (s *GraphService) ClearPendingConnections(ctx context.Context, workspaceID string) (int64, error) {
	// pending 상태인 모든 연결을 삭제한다.
	result, err := s.db.Collection("graph_connections").DeleteMany(
		ctx,
		bson.M{"workspace_id": workspaceID, "status": StatusPending},
	)
	if err != nil {
		return 0, fmt.Errorf("failed to clear pending connections: %w", err)
	}

	log.Printf("[ClearConnections] Success: Deleted %d pending/edited connections for WS ID: %s", result.DeletedCount, workspaceID)
	return result.DeletedCount, nil
}

func (s *GraphService) GetWorkspaceGraph(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	cursor, err := s.db.Collection("graph_connections").Find(ctx, bson.M{"workspace_id": workspaceID})
	if err != nil {
		return nil, fmt.Errorf("failed to fetch workspace graph: %w", err)
	}
	defer func() { _ = cursor.Close(ctx) }()

	var connections []models.GraphConnection
	if err := cursor.All(ctx, &connections); err != nil {
		return nil, fmt.Errorf("failed to decode workspace graph: %w", err)
	}
	return connections, nil
}

func (s *GraphService) GetWorkspaceGraphResponse(ctx context.Context, workspaceID string) (*models.WorkspaceGraphResponse, error) {
	connections, err := s.GetWorkspaceGraph(ctx, workspaceID)
	if err != nil {
		return nil, err
	}

	nodesMap := make(map[string]models.GraphNode)
	for _, conn := range connections {
		nodesMap[conn.SourceID.Hex()] = models.GraphNode{ID: conn.SourceID.Hex(), Title: ""}
		nodesMap[conn.TargetID.Hex()] = models.GraphNode{ID: conn.TargetID.Hex(), Title: ""}
	}

	nodes := make([]models.GraphNode, 0, len(nodesMap))
	for _, n := range nodesMap {
		nodes = append(nodes, n)
	}

	edges := make([]models.GraphEdge, len(connections))
	for i, conn := range connections {
		edges[i] = models.GraphEdge{
			SourceID: conn.SourceID.Hex(),
			TargetID: conn.TargetID.Hex(),
			Status:   conn.Status,
		}
	}

	return &models.WorkspaceGraphResponse{
		Nodes: nodes,
		Edges: edges,
	}, nil
}
