package services_test

import (
	"context"
	"testing"

	"graph/models"
	"graph/services"

	"github.com/stretchr/testify/assert"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/integration/mtest"
)

type MockGraphService struct {
	AutoConnectWorkspaceFunc    func(ctx context.Context, workspaceID string) ([]models.GraphConnection, error)
	ClearPendingConnectionsFunc func(ctx context.Context, workspaceID string) ([]models.GraphConnection, error)
}

func (m *MockGraphService) NoteCreated(ctx context.Context, newDocID, workspaceID string) ([]models.GraphConnection, error) {
	return nil, nil
}
func (m *MockGraphService) AutoConnectWorkspace(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	if m.AutoConnectWorkspaceFunc != nil {
		return m.AutoConnectWorkspaceFunc(ctx, workspaceID)
	}
	return nil, nil
}
func (m *MockGraphService) ConfirmGraphConnection(ctx context.Context, sourceID, targetID string) error {
	return nil
}
func (m *MockGraphService) ConfirmAllConnections(ctx context.Context, workspaceID string) error {
	return nil
}
func (m *MockGraphService) NoteDeleted(ctx context.Context, docID string) error {
	return nil
}
func (m *MockGraphService) GetWorkspaceGraphResponse(ctx context.Context, workspaceID string) (*models.WorkspaceGraphResponse, error) {
	return nil, nil
}
func (m *MockGraphService) ClearPendingConnections(ctx context.Context, workspaceID string) ([]models.GraphConnection, error) {
	if m.ClearPendingConnectionsFunc != nil {
		return m.ClearPendingConnectionsFunc(ctx, workspaceID)
	}
	return nil, nil
}

func TestWorkspaceService_ChangeWorkspaceStyle(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))

	const (
		workspaceID = "6517a2624a081a27e7d0f91e"
		userID      = "testuser123"
	)
	ctx := context.Background()

	mt.Run("Error_InvalidInput", func(mt *mtest.T) {
		service := services.NewWorkspaceService(mt.DB, nil)

		_, err := service.ChangeWorkspaceStyle(ctx, "", userID, "zettel")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "workspaceID가 비어있습니다")

		_, err = service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "newStyle이 비어있습니다")

		_, err = service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "unsupported")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "지원하지 않는 스타일입니다")
	})

	mt.Run("Error_GraphServiceNil", func(mt *mtest.T) {
		service := services.NewWorkspaceService(mt.DB, nil)

		mt.AddMockResponses(
			bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 1}, {Key: "nModified", Value: 1}},
		)

		_, err := service.ChangeWorkspaceStyle(ctx, workspaceID, userID, models.WorkspaceTypeZettel)
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "graphService가 주입되지 않았습니다")
	})

	mt.Run("Error_UpdateWorkspaceFails", func(mt *mtest.T) {
		mt.AddMockResponses(
			mtest.CreateWriteErrorsResponse(mtest.WriteError{Code: 11000, Message: "update failed"}),
		)

		service := services.NewWorkspaceService(mt.DB, &services.GraphService{})
		_, err := service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "zettel")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "워크스페이스 타입 업데이트 실패")
	})
}
