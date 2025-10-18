package server

import (
	"graph/handlers"
	"graph/services"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func NewServer(gs *services.GraphService, ws *services.WorkspaceService) *gin.Engine {
	router := gin.Default()

	graphHandler := handlers.NewGraphConnectionHandler(gs)
	workspaceHandler := handlers.NewWorkspaceGraphHandler(gs, ws)

	// Graph
	router.POST("/api/connections/confirm", graphHandler.ConfirmGraphConnection)
	router.POST("/api/connections/edit", graphHandler.EditGraphConnection)
	router.POST("/api/connections/note-deleted", graphHandler.DeleteNoteGraph)

	// Workspace
	router.POST("/api/workspaces", workspaceHandler.CreateWorkspace)
	router.PUT("/api/workspaces/:workspaceId", workspaceHandler.UpdateWorkspace)
	router.DELETE("/api/workspaces/:workspaceId", workspaceHandler.DeleteWorkspace)
	router.GET("/api/workspaces/:workspaceId/graph", workspaceHandler.GetWorkspaceGraph)
	router.POST("/api/workspaces/:workspaceId/style", workspaceHandler.ChangeWorkspaceStyle)
	router.POST("/api/workspaces/:workspaceId/confirm-all", workspaceHandler.ConfirmAllConnections)
	router.GET("/api/workspaces/:workspaceId/type", workspaceHandler.GetWorkspaceType)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	return router
}
