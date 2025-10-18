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

	// 그래프 연결 관리
	router.POST("/api/graphs/connect/confirm", graphHandler.ConfirmGraphConnection)
	router.POST("/api/graphs/connect/edit", graphHandler.EditGraphConnection)

	// 워크스페이스 그래프 조회
	router.GET("/api/workspaces/:workspaceId/graph", workspaceHandler.GetWorkspaceGraph)

	// 워크스페이스 타입 변환
	router.POST("/api/workspaces/:workspaceId/change-style", workspaceHandler.ChangeWorkspaceStyle)

	// 워크스페이스 내 모든 미확정 연결 확정
	router.POST("/api/workspaces/:workspaceId/confirm-all", workspaceHandler.ConfirmAllConnections)

	// 워크스페이스가 존재하면 타입을 반환
	router.GET("/:workspaceId/type", workspaceHandler.GetWorkspaceType)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	return router
}
