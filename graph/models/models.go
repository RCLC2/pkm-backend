package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Document struct {
	ID           primitive.ObjectID `bson:"_id,omitempty" json:"id"`
	WorkspaceID  string             `bson:"workspace_id" json:"workspaceId"`
	Title        string             `bson:"title" json:"title"`
	Content      string             `bson:"content" json:"content"`
	ParaCategory string             `bson:"para_category,omitempty" json:"paraCategory"`
	CreatedAt    time.Time          `bson:"created_at" json:"createdAt"`
	UpdatedAt    time.Time          `bson:"updated_at" json:"updatedAt"`
}

type GraphConnection struct {
	ID          primitive.ObjectID `bson:"_id,omitempty" json:"id"`
	SourceID    primitive.ObjectID `bson:"source_id" json:"sourceId"`
	TargetID    primitive.ObjectID `bson:"target_id" json:"targetId"`
	Status      string             `bson:"status" json:"status"` // "pending", "confirmed", "edited"
	WorkspaceID string             `bson:"workspace_id" json:"workspaceId"`
	CreatedAt   time.Time          `bson:"created_at" json:"createdAt"`
	UpdatedAt   time.Time          `bson:"updated_at" json:"updatedAt"`
}

type GraphNode struct {
	ID    string `json:"id"`
	Title string `json:"title"`
}

type GraphEdge struct {
	SourceID string `json:"sourceId"`
	TargetID string `json:"targetId"`
	Status   string `json:"status"` // "pending", "confirmed", "edited"
}

type WorkspaceGraphResponse struct {
	Nodes []GraphNode `json:"nodes"`
	Edges []GraphEdge `json:"edges"`
}
