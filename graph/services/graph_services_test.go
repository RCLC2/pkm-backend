package services_test

import (
	"context"
	"errors"
	"graph/services"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo/integration/mtest"
)

func objIDFromHex(t *testing.T, hex string) primitive.ObjectID {
	objID, err := primitive.ObjectIDFromHex(hex)
	assert.NoError(t, err)
	return objID
}

func TestGraphService_NoteCreated(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))

	workspaceID := "6517a2624a081a27e7d0f91e"
	newDocID := "6517a2624a081a27e7d0f92a"
	similarID1 := "6517a2624a081a27e7d0f92b"
	similarID2 := "6517a2624a081a27e7d0f92c"

	ctx := context.Background()

	mt.Run("Success_CreatesPendingConnections", func(mt *mtest.T) {
		mockFetchSimilar := func(_ context.Context, docID string, topN int) ([]string, error) {
			assert.Equal(t, newDocID, docID)
			assert.Equal(t, 5, topN)
			return []string{similarID1, similarID2, newDocID, "invalid-id"}, nil
		}

		service := services.NewGraphService(mt.DB)
		service.FetchSimilarByID = mockFetchSimilar

		mt.AddMockResponses(
			mtest.CreateSuccessResponse(),
			mtest.CreateSuccessResponse(),
		)

		connections, err := service.NoteCreated(ctx, newDocID, workspaceID)
		assert.NoError(t, err)
		assert.Len(t, connections, 2, "유효한 연결은 2개만 생성되어야 합니다 (자기 자신, invalid-id 스킵)")
		assert.Equal(t, services.StatusPending, connections[0].Status)

		assert.Equal(t, objIDFromHex(t, newDocID), connections[0].SourceID)
		assert.Equal(t, objIDFromHex(t, similarID1), connections[0].TargetID)
	})

	mt.Run("Error_InvalidNewDocID", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		_, err := service.NoteCreated(ctx, "invalid-id", workspaceID)
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "invalid new document ID")
	})

	mt.Run("Error_FetchSimilarFailed", func(mt *mtest.T) {
		mockFetchSimilar := func(_ context.Context, _ string, _ int) ([]string, error) {
			return nil, errors.New("external topic service error")
		}

		service := services.NewGraphService(mt.DB)
		service.FetchSimilarByID = mockFetchSimilar

		_, err := service.NoteCreated(ctx, newDocID, workspaceID)
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "failed to fetch similar docs by ID: external topic service error")
	})
}

func TestGraphService_AutoConnectWorkspace(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))

	workspaceID := "6517a2624a081a27e7d0f91e"
	docID1 := "6517a2624a081a27e7d0f92a"
	docID2 := "6517a2624a081a27e7d0f92b"
	docID3 := "6517a2624a081a27e7d0f92c"

	ctx := context.Background()

	mt.Run("Success_UpsertsConnections", func(mt *mtest.T) {
		mockFetchDocIDs := func(_ context.Context, wsID string) ([]string, error) {
			assert.Equal(t, workspaceID, wsID)
			return []string{docID1, docID2}, nil
		}

		mockFetchSimilar := func(_ context.Context, docID string, _ int) ([]string, error) {
			if docID == docID1 {
				return []string{docID2, docID3}, nil // doc1 -> doc2, doc1 -> doc3
			} else if docID == docID2 {
				return []string{docID1, docID2}, nil // doc2 -> doc1 (doc2 skip)
			}
			return nil, nil
		}

		service := services.NewGraphService(mt.DB)
		service.FetchSimilarByID = mockFetchSimilar
		service.FetchDocumentIDs = mockFetchDocIDs

		mt.AddMockResponses(
			// docID1:
			mtest.CreateSuccessResponse(bson.E{Key: "ok", Value: 1}, bson.E{Key: "nModified", Value: 1}), // doc1 -> doc2
			mtest.CreateSuccessResponse(bson.E{Key: "ok", Value: 1}, bson.E{Key: "nModified", Value: 0}), // doc1 -> doc3
			// docID2:
			mtest.CreateSuccessResponse(bson.E{Key: "ok", Value: 1}, bson.E{Key: "nModified", Value: 1}), // doc2 -> doc1
		)

		connections, err := service.AutoConnectWorkspace(ctx, workspaceID)
		assert.NoError(t, err)
		assert.Len(t, connections, 3, "총 3개의 연결이 생성/업데이트되어야 합니다.")

		assert.Equal(t, services.StatusPending, connections[0].Status)

		// doc1 -> doc2
		assert.Equal(t, objIDFromHex(t, docID1), connections[0].SourceID)
		assert.Equal(t, objIDFromHex(t, docID2), connections[0].TargetID)

		// doc2 -> doc1
		assert.Equal(t, objIDFromHex(t, docID2), connections[2].SourceID)
		assert.Equal(t, objIDFromHex(t, docID1), connections[2].TargetID)
	})

	mt.Run("Error_FetchDocumentIDsFailed", func(mt *mtest.T) {
		mockFetchDocIDs := func(_ context.Context, _ string) ([]string, error) {
			return nil, errors.New("external document service error")
		}

		service := services.NewGraphService(mt.DB)
		service.FetchDocumentIDs = mockFetchDocIDs

		_, err := service.AutoConnectWorkspace(ctx, workspaceID)
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "failed to fetch workspace doc IDs: external document service error")
	})

	mt.Run("PartialSuccess_FetchSimilarFailed", func(mt *mtest.T) {
		mockFetchDocIDs := func(_ context.Context, _ string) ([]string, error) {
			return []string{docID1, docID2}, nil
		}

		mockFetchSimilar := func(_ context.Context, docID string, _ int) ([]string, error) {
			if docID == docID1 {
				return []string{docID3}, nil // doc1 -> doc3
			}
			return nil, errors.New("doc2 similar search failed")
		}

		service := services.NewGraphService(mt.DB)
		service.FetchDocumentIDs = mockFetchDocIDs
		service.FetchSimilarByID = mockFetchSimilar

		mt.AddMockResponses(
			mtest.CreateSuccessResponse(bson.E{Key: "ok", Value: 1}, bson.E{Key: "nModified", Value: 1}),
		)

		connections, err := service.AutoConnectWorkspace(ctx, workspaceID)
		assert.NoError(t, err)
		assert.Len(t, connections, 1, "docID2 실패로 1개 연결만 생성되어야 합니다.")
		assert.Equal(t, objIDFromHex(t, docID1), connections[0].SourceID)
		assert.Equal(t, objIDFromHex(t, docID3), connections[0].TargetID)
	})
}

func TestGraphService_ConfirmGraphConnection(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))
	sourceID := "6517a2624a081a27e7d0f93a"
	targetID := "6517a2624a081a27e7d0f93b"
	ctx := context.Background()

	mt.Run("Success_UpdatesToConfirmed", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		mt.AddMockResponses(
			bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 1}, {Key: "nModified", Value: 1}},
		)

		err := service.ConfirmGraphConnection(ctx, sourceID, targetID)
		assert.NoError(t, err)
	})

	mt.Run("Error_InvalidSourceID", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		err := service.ConfirmGraphConnection(ctx, "invalid", targetID)
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "invalid source ID")
	})
}

func TestGraphService_ConfirmAllConnections(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))
	workspaceID := "6517a2624a081a27e7d0f91e"
	ctx := context.Background()

	mt.Run("Success_UpdatesMultipleConnections", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		mt.AddMockResponses(
			bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 5}, {Key: "nModified", Value: 3}},
		)

		err := service.ConfirmAllConnections(ctx, workspaceID)
		assert.NoError(t, err)
	})
}

func TestGraphService_NoteDeleted(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))
	docID := "6517a2624a081a27e7d0f93c"
	ctx := context.Background()

	mt.Run("Success_DeletesRelatedConnections", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		mt.AddMockResponses(
			bson.D{{Key: "ok", Value: 1}, {Key: "n", Value: 1}, {Key: "nModified", Value: 0}, {Key: "deletedCount", Value: 2}},
		)

		err := service.NoteDeleted(ctx, docID)
		assert.NoError(t, err)
	})

	mt.Run("Error_InvalidDocID", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)
		err := service.NoteDeleted(ctx, "invalid")
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "invalid document ID")
	})
}

func TestGraphService_GetWorkspaceGraphResponse(t *testing.T) {
	mt := mtest.New(t, mtest.NewOptions().ClientType(mtest.Mock))

	workspaceID := "6517a2624a081a27e7d0f91e"
	docID1 := objIDFromHex(t, "6517a2624a081a27e7d0f92a")
	docID2 := objIDFromHex(t, "6517a2624a081a27e7d0f92b")

	ctx := context.Background()

	mt.Run("Success_ReturnsNodesAndEdges", func(mt *mtest.T) {
		service := services.NewGraphService(mt.DB)

		cursorResponse := mtest.CreateCursorResponse(
			1,
			"graph_db.graph_connections",
			mtest.FirstBatch,
			bson.D{
				{Key: "_id", Value: primitive.NewObjectID()},
				{Key: "source_id", Value: docID1},
				{Key: "target_id", Value: docID2},
				{Key: "status", Value: services.StatusConfirmed},
				{Key: "workspace_id", Value: workspaceID},
				{Key: "created_at", Value: time.Now()},
				{Key: "updated_at", Value: time.Now()},
			},
			bson.D{
				{Key: "_id", Value: primitive.NewObjectID()},
				{Key: "source_id", Value: docID2},
				{Key: "target_id", Value: docID1},
				{Key: "status", Value: services.StatusEdited},
				{Key: "workspace_id", Value: workspaceID},
				{Key: "created_at", Value: time.Now()},
				{Key: "updated_at", Value: time.Now()},
			},
		)
		mt.AddMockResponses(cursorResponse)

		response, err := service.GetWorkspaceGraphResponse(ctx, workspaceID)
		assert.NoError(t, err)
		assert.NotNil(t, response)

		assert.Len(t, response.Nodes, 2)
		assert.Len(t, response.Edges, 2)

		assert.Equal(t, services.StatusConfirmed, response.Edges[0].Status)
		assert.Equal(t, services.StatusEdited, response.Edges[1].Status)
	})
}
