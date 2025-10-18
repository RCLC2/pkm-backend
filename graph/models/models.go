package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

const (
	WorkspaceTypeGeneric = "generic"
	WorkspaceTypePara    = "para"
	WorkspaceTypeZettel  = "zettel"
)

type Workspace struct {
	ID        primitive.ObjectID `bson:"_id,omitempty" json:"id"`
	Title     string             `bson:"title" json:"title"`
	Type      string             `bson:"type" json:"type"` // generic, para, zettel
	UserID    string             `bson:"user_id" json:"userId"`
	CreatedAt time.Time          `bson:"created_at" json:"createdAt"`
	UpdatedAt time.Time          `bson:"updated_at" json:"updatedAt"`
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
