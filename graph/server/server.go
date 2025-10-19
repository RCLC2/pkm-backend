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
	router.POST("/connections/confirm", graphHandler.ConfirmGraphConnection)
	router.POST("/connections/edit", graphHandler.EditGraphConnection)
	router.POST("/connections/note-deleted", graphHandler.DeleteNoteGraph)

	// Workspace
	router.POST("/workspaces", workspaceHandler.CreateWorkspace)
	router.PUT("/workspaces/:workspaceId", workspaceHandler.UpdateWorkspace)
	router.DELETE("/workspaces/:workspaceId", workspaceHandler.DeleteWorkspace)
	router.GET("/workspaces/:workspaceId/graph", workspaceHandler.GetWorkspaceGraph)
	router.POST("/workspaces/:workspaceId/style", workspaceHandler.ChangeWorkspaceStyle)
	router.POST("/workspaces/:workspaceId/confirm-all", workspaceHandler.ConfirmAllConnections)
	router.GET("/workspaces/:workspaceId/type", workspaceHandler.GetWorkspaceType)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	return router
}
