package services

import (
	"context"
	"testing"
	"time"

	"graph/models"

	"github.com/stretchr/testify/assert"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
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
		service := NewWorkspaceService(mt.DB, nil, nil, "")

		_, err := service.ChangeWorkspaceStyle(ctx, "", userID, "zettel")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "workspaceID is blank")

		_, err = service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "newStyle is blank")

		_, err = service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "unsupported")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "invalid workspace type.")
	})

	mt.Run("Error_GraphServiceNil", func(mt *mtest.T) {
		service := NewWorkspaceService(mt.DB, nil, nil, "")

		mt.AddMockResponses(
			bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 1}, {Key: "nModified", Value: 1}},
		)

		_, err := service.ChangeWorkspaceStyle(ctx, workspaceID, userID, models.WorkspaceTypeZettel)
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "graphService is nil")
	})

	mt.Run("Error_UpdateWorkspaceFails", func(mt *mtest.T) {
		mt.AddMockResponses(
			mtest.CreateWriteErrorsResponse(mtest.WriteError{Code: 11000, Message: "update failed"}),
		)

		service := NewWorkspaceService(mt.DB, &GraphService{}, nil, "")
		_, err := service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "zettel")
		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "failed to update workspace type")
	})

	mt.Run("Error_InsertJobFails", func(mt *mtest.T) {
		wsID, _ := primitive.ObjectIDFromHex(workspaceID)

		findResponse := mtest.CreateCursorResponse(1, "testdb.workspaces", mtest.FirstBatch, bson.D{
			{Key: "_id", Value: wsID},
			{Key: "title", Value: "Original Title"},
			{Key: "type", Value: models.WorkspaceTypeGeneric},
			{Key: "user_id", Value: userID},
			{Key: "yorkie_project_id", Value: "proj-abc"},
			{Key: "created_at", Value: time.Now()},
			{Key: "updated_at", Value: time.Now()},
		})

		updateResponse := bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 1}, {Key: "nModified", Value: 1}}
		insertJobFailResponse := mtest.CreateWriteErrorsResponse(mtest.WriteError{Code: 11000, Message: "insert job failed"})
		mt.AddMockResponses(findResponse, updateResponse, insertJobFailResponse)

		service := NewWorkspaceService(mt.DB, &GraphService{}, nil, "")
		_, err := service.ChangeWorkspaceStyle(ctx, workspaceID, userID, "zettel")

		assert.Error(mt, err)
		assert.Contains(mt, err.Error(), "insert job failed")
	})
}

func TestWorkspaceService_FindAllWorkspacesByUserID(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))
	ctx := context.Background()
	const testUserID = "user-abc-123"

	mt.Run("Success_FindsMultipleWorkspaces", func(mt *mtest.T) {
		service := NewWorkspaceService(mt.DB, nil, nil, "")
		wsID1 := primitive.NewObjectID()
		wsID2 := primitive.NewObjectID()
		now := time.Now()

		// todo
		first := mtest.CreateCursorResponse(1, "testdb.workspaces", mtest.FirstBatch, bson.D{
			{Key: "_id", Value: wsID1},
			{Key: "title", Value: "Workspace A"},
			{Key: "type", Value: models.WorkspaceTypeGeneric},
			{Key: "user_id", Value: testUserID},
			{Key: "yorkie_project_id", Value: "proj-a"},
			{Key: "created_at", Value: now},
		})
		second := mtest.CreateCursorResponse(1, "testdb.workspaces", mtest.NextBatch, bson.D{
			{Key: "_id", Value: wsID2},
			{Key: "title", Value: "Workspace B"},
			{Key: "type", Value: models.WorkspaceTypeZettel},
			{Key: "user_id", Value: testUserID},
			{Key: "yorkie_project_id", Value: "proj-b"},
			{Key: "created_at", Value: now},
		})
		end := mtest.CreateCursorResponse(0, "testdb.workspaces", mtest.NextBatch)

		mt.AddMockResponses(first, second, end)

		workspaces, err := service.FindAllWorkspacesByUserID(ctx, testUserID)
		assert.NoError(mt, err)
		assert.Len(mt, workspaces, 2)

		assert.Equal(mt, "Workspace A", workspaces[0].Title)
		assert.Equal(mt, "Workspace B", workspaces[1].Title)
		assert.Equal(mt, testUserID, workspaces[0].UserID)
	})

	mt.Run("Success_NoWorkspacesFound", func(mt *mtest.T) {
		service := NewWorkspaceService(mt.DB, nil, nil, "")
		mt.AddMockResponses(mtest.CreateCursorResponse(0, "testdb.workspaces", mtest.FirstBatch))

		workspaces, err := service.FindAllWorkspacesByUserID(ctx, "non-existent-user")
		assert.NoError(mt, err)
		assert.Empty(mt, workspaces)
	})

	mt.Run("Error_BlankUserID", func(mt *mtest.T) {
		service := NewWorkspaceService(mt.DB, nil, nil, "")
		workspaces, err := service.FindAllWorkspacesByUserID(ctx, "")
		assert.Error(mt, err)
		assert.Nil(mt, workspaces)
		assert.Contains(mt, err.Error(), "userID is blank")
	})

	mt.Run("Error_MongoFindFails", func(mt *mtest.T) {
		service := NewWorkspaceService(mt.DB, nil, nil, "")
		mt.AddMockResponses(mtest.CreateCommandErrorResponse(mtest.CommandError{
			Code:    1,
			Message: "internal server error",
		}))

		workspaces, err := service.FindAllWorkspacesByUserID(ctx, testUserID)
		assert.Error(mt, err)
		assert.Nil(mt, workspaces)
		assert.Contains(mt, err.Error(), "failed to find workspaces")
	})
}

func TestGenerateProjectName(t *testing.T) {
	tests := []struct {
		userID string
		title  string
	}{
		{"google-sub-12345", "안녕하세요 월드"},
		{"u1", "Hello World!"},
		{"user12345678901234567888080808080808090", "테스트 프로젝트 이름이 길어요"},
		{"abc", "Special & Characters ** Test"},
	}

	const maxLen = 30

	for _, tt := range tests {
		got := generateProjectName(tt.userID, tt.title)
		if len([]rune(got)) > maxLen {
			t.Errorf("generateProjectName(%q, %q) = %q; length %d exceeds %d",
				tt.userID, tt.title, got, len([]rune(got)), maxLen)
		} else {
			t.Logf("PASS: %q + %q -> %q (len=%d)", tt.userID, tt.title, got, len([]rune(got)))
		}
	}
}
