package services

import (
	"context"
	"errors"
	"graph/models"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type MockGraphService struct {
	GraphService
	Documents        []models.Document
	GraphConnections []models.GraphConnection
	FailFetch        bool
}

func (m *MockGraphService) fetchSimilarDocs(ctx context.Context, content string, topN int) ([]string, error) {
	if m.FailFetch {
		return nil, errors.New("failed to fetch similar docs")
	}

	return []string{primitive.NewObjectID().Hex(), primitive.NewObjectID().Hex()}, nil
}

func (m *MockGraphService) AutoConnectWorkspace(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	var connections []models.GraphConnection
	for _, doc := range m.Documents {
		if doc.WorkspaceID != workspaceID {
			continue
		}

		similarIDs, err := m.fetchSimilarDocs(ctx, doc.Content, 5)
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
			connections = append(connections, conn)
			m.GraphConnections = append(m.GraphConnections, conn)
		}
	}
	return connections, nil
}

func (m *MockGraphService) ConfirmAllConnections(ctx context.Context, workspaceID string) error {
	for i := range m.GraphConnections {
		if m.GraphConnections[i].WorkspaceID == workspaceID &&
			(m.GraphConnections[i].Status == "pending" || m.GraphConnections[i].Status == "edit") {
			m.GraphConnections[i].Status = "confirmed"
			m.GraphConnections[i].UpdatedAt = time.Now()
		}
	}
	return nil
}

func TestAutoConnectWorkspace(t *testing.T) {
	mockService := &MockGraphService{
		Documents: []models.Document{
			{ID: primitive.NewObjectID(), WorkspaceID: "ws1", Content: "doc1"},
			{ID: primitive.NewObjectID(), WorkspaceID: "ws1", Content: "doc2"},
		},
	}

	connections, err := mockService.AutoConnectWorkspace(context.Background(), "ws1")
	assert.NoError(t, err)
	assert.Len(t, connections, 4) // 2 documents × 2 connections
	for _, conn := range connections {
		assert.Equal(t, "pending", conn.Status)
		assert.Equal(t, "ws1", conn.WorkspaceID)
	}
}

func TestAutoConnectWorkspace_FailFetch(t *testing.T) {
	mockService := &MockGraphService{
		FailFetch: true,
		Documents: []models.Document{
			{ID: primitive.NewObjectID(), WorkspaceID: "ws1", Content: "doc1"},
		},
	}

	connections, err := mockService.AutoConnectWorkspace(context.Background(), "ws1")
	assert.NoError(t, err)
	assert.Len(t, connections, 0)
}

func TestConfirmAllConnections(t *testing.T) {
	mockService := &MockGraphService{
		GraphConnections: []models.GraphConnection{
			{Status: "pending", WorkspaceID: "ws1"},
			{Status: "edit", WorkspaceID: "ws1"},
			{Status: "confirmed", WorkspaceID: "ws1"},
			{Status: "pending", WorkspaceID: "ws2"},
		},
	}

	err := mockService.ConfirmAllConnections(context.Background(), "ws1")
	assert.NoError(t, err)

	countConfirmed := 0
	for _, c := range mockService.GraphConnections {
		if c.WorkspaceID == "ws1" {
			assert.Equal(t, "confirmed", c.Status)
			countConfirmed++
		}
	}

	assert.Equal(t, 3, countConfirmed) // 이 워크스페이스 내에서 모든 연결이 확정 상태로 변경
}
