package server

import (
	"graph/handlers"
	"graph/services"

	_ "graph/docs"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func NewServer(gs *services.GraphService, ws *services.WorkspaceService) *gin.Engine {
	router := gin.Default()

	graphHandler := handlers.NewGraphConnectionHandler(gs)
	workspaceHandler := handlers.NewWorkspaceGraphHandler(gs, ws)

	// 그래프 연결 관리
	router.POST("/connections/confirm", graphHandler.ConfirmGraphConnection)
	router.POST("/connections/edit", graphHandler.EditGraphConnection)
	router.POST("/connections/note-deleted", graphHandler.DeleteNoteGraph)
	router.POST("/workspaces/:workspaceId/clear-pending", graphHandler.ClearPendingConnections)

	// 워크스페이스 관리
	router.GET("/workspaces", workspaceHandler.FindAllWorkspaces)
	router.POST("/workspaces", workspaceHandler.CreateWorkspace)
	router.PUT("/workspaces/:workspaceId", workspaceHandler.UpdateWorkspace)
	router.DELETE("/workspaces/:workspaceId", workspaceHandler.DeleteWorkspace)

	// 워크스페이스 그래프 조회
	router.GET("/workspaces/:workspaceId/graph", workspaceHandler.GetWorkspaceGraph)

	// 워크스페이스 타입 변환
	router.POST("/workspaces/:workspaceId/style", workspaceHandler.ChangeWorkspaceStyle)

	// 워크스페이스 내 모든 미확정 연결 확정
	router.POST("/workspaces/:workspaceId/confirm-all", workspaceHandler.ConfirmAllConnections)

	// 워크스페이스가 존재하면 타입을 반환
	router.GET("/workspaces/:workspaceId/type", workspaceHandler.GetWorkspaceType)

	// 사용자의 모든 워크스페이스를 반환
	router.GET("/workspaces/user/:userId", workspaceHandler.FindAllWorkspaces)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	return router
}
