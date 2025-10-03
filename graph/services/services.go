package services

import (
	"bytes"
	"context"
	"encoding/json"
	"graph/models"
	"io/ioutil"
	"net/http"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type GraphService struct {
	db *mongo.Database
}

func NewGraphService(db *mongo.Database) *GraphService {
	return &GraphService{db: db}
}

func (s *GraphService) SetParaCategory(ctx context.Context, docID string, category string) error {
	objID, err := primitive.ObjectIDFromHex(docID)
	if err != nil {
		return err
	}
	_, err = s.db.Collection("documents").UpdateOne(ctx,
		bson.M{"_id": objID},
		bson.M{"$set": bson.M{"para_category": category, "updated_at": time.Now()}},
	)
	return err
}

func (s *GraphService) ConvertToZettelkasten(ctx context.Context, docID string) error {
	objID, err := primitive.ObjectIDFromHex(docID)
	if err != nil {
		return err
	}
	_, err = s.db.Collection("documents").UpdateOne(ctx,
		bson.M{"_id": objID},
		bson.M{"$unset": bson.M{"para_category": ""}},
	)
	return err
}

func fetchSimilarDocsHTTP(ctx context.Context, content string, topN int) ([]string, error) {
	reqBody, _ := json.Marshal(map[string]interface{}{
		"content": content,
		"topN":    topN,
	})

	req, _ := http.NewRequestWithContext(ctx, "POST", "http://localhost:8083/api/find-similar", bytes.NewReader(reqBody))
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)

	var result struct {
		IDs []string `json:"ids"`
	}
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, err
	}

	return result.IDs, nil
}

func (s *GraphService) ConfirmGraphConnection(ctx context.Context, sourceID, targetID string) error {
	sourceObjID, _ := primitive.ObjectIDFromHex(sourceID)
	targetObjID, _ := primitive.ObjectIDFromHex(targetID)

	_, err := s.db.Collection("graph_connections").UpdateOne(
		ctx,
		bson.M{"source_id": sourceObjID, "target_id": targetObjID},
		bson.M{"$set": bson.M{"status": "confirmed", "updated_at": time.Now()}},
	)
	return err
}

func (s *GraphService) EditGraphConnection(ctx context.Context, sourceID, targetID string) error {
	sourceObjID, _ := primitive.ObjectIDFromHex(sourceID)
	targetObjID, _ := primitive.ObjectIDFromHex(targetID)

	_, err := s.db.Collection("graph_connections").UpdateOne(
		ctx,
		bson.M{"source_id": sourceObjID, "target_id": targetObjID},
		bson.M{"$set": bson.M{"status": "edited", "updated_at": time.Now()}},
	)
	return err
}

func (s *GraphService) GetWorkspaceGraph(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	var connections []models.GraphConnection
	cursor, err := s.db.Collection("graph_connections").Find(ctx, bson.M{"workspace_id": workspaceID})
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)
	if err := cursor.All(ctx, &connections); err != nil {
		return nil, err
	}
	return connections, nil
}

// todo. 캐시 도입 논의
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

// PARA to ZETTELKASTEN

func (s *GraphService) AutoConnectWorkspace(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	cursor, err := s.db.Collection("documents").Find(ctx, bson.M{"workspace_id": workspaceID})
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var connections []models.GraphConnection
	for cursor.Next(ctx) {
		var doc models.Document
		if err := cursor.Decode(&doc); err != nil {
			continue
		}

		similarIDs, err := fetchSimilarDocsHTTP(ctx, doc.Content, 5)
		if err != nil {
			continue
		}

		for _, targetIDStr := range similarIDs {
			targetID, _ := primitive.ObjectIDFromHex(targetIDStr)
			conn := models.GraphConnection{
				SourceID:    doc.ID,
				TargetID:    targetID,
				Status:      "pending",
				WorkspaceID: workspaceID,
				CreatedAt:   time.Now(),
				UpdatedAt:   time.Now(),
			}
			_, _ = s.db.Collection("graph_connections").UpdateOne(
				ctx,
				bson.M{"source_id": doc.ID, "target_id": targetID},
				bson.M{"$set": conn},
				options.Update().SetUpsert(true),
			)
			connections = append(connections, conn)
		}
	}
	return connections, nil
}

func (s *GraphService) ConfirmAllConnections(ctx context.Context, workspaceID string) error {
	_, err := s.db.Collection("graph_connections").UpdateMany(
		ctx,
		bson.M{"workspace_id": workspaceID, "status": bson.M{"$in": []string{"pending", "edit"}}},
		bson.M{"$set": bson.M{"status": "confirmed", "updated_at": time.Now()}},
	)
	return err
}
