package server

import (
	"graph/handlers"
	"graph/services"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func NewServer(gs *services.GraphService) *gin.Engine {
	router := gin.Default()

	graphHandler := handlers.NewGraphHandler(gs)

	// 문서 분류 및 변환
	router.POST("/api/documents/para", graphHandler.SetParaCategory)
	router.POST("/api/documents/:documentId/zettelkasten", graphHandler.ConvertToZettelkasten)

	// 그래프 연결 관리
	router.POST("/api/graphs/connect/pending", graphHandler.ConfirmGraphConnection)
	router.POST("/api/graphs/connect/confirm", graphHandler.ConfirmGraphConnection)
	router.POST("/api/graphs/connect/edit", graphHandler.EditGraphConnection)

	// 워크스페이스 조회
	router.GET("/api/workspaces/:workspaceId/zettel", graphHandler.GetWorkspaceGraph)

	// 워크스페이스 내 모든 문서 자동 연결
	router.POST("/api/workspaces/:workspaceId/connect/auto", graphHandler.AutoConnectWorkspace)

	// 워크스페이스 내 모든 미확정 연결 확정
	router.POST("/api/workspaces/:workspaceId/connect/confirm-all", graphHandler.ConfirmAllConnections)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	return router
}
