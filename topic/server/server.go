package server

import (
	"topic/esclient"
	"topic/handlers"

	_ "topic/docs"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func NewServer(es *esclient.ElasticService) *gin.Engine {
	router := gin.Default()
	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	h := handlers.NewHandler(es)

	router.POST("/extract-tags", h.ExtractTags)
	router.POST("/find-similar/by-content", h.FindSimilarDocsByContent)
	router.GET("/find-similar/by-id", h.FindSimilarDocsById)

	return router
}
