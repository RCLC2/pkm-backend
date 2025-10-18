package handlers

import (
	"net/http"
	"topic/esclient"

	"github.com/gin-gonic/gin"
)

type SearchRequest struct {
	Content string `json:"content" binding:"required"`
	TopN    int    `json:"topN,omitempty"`
}

type Handler struct {
	es *esclient.ElasticService
}

func NewHandler(es *esclient.ElasticService) *Handler {
	return &Handler{es: es}
}

// ExtractTags godoc
// @Summary 태그 추출
// @Description 문서 내에서 빈도 기반 토큰화, 상위 N개의 태그를 반환
// @Accept json
// @Produce json
// @Param request body SearchRequest true "콘텐츠와 상위 N개 태그 개수(default=10)"
// @Router /api/extract-tags [post]
func (h *Handler) ExtractTags(c *gin.Context) {
	var req SearchRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	tags, err := h.es.ExtractTopTags(c.Request.Context(), req.Content, req.TopN)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"status": "success",
		"tags":   tags,
	})
}

// FindSimilarDocs godoc
// @Summary 유사한 문서 탐색
// @Description 문서와 유사한 문서 탐색
// @Accept json
// @Produce json
// @Param request body SearchRequest true "콘텐츠"
// @Router /api/find-similar [post]
func (h *Handler) FindSimilarDocsByContent(c *gin.Context) {
	var req SearchRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ids, err := h.es.FindSimilarDocsByContent(c.Request.Context(), req.Content)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"status": "success",
		"ids":    ids,
	})
}

// FindSimilarDocs godoc
// @Summary 유사한 문서 탐색
// @Description 문서와 유사한 문서 탐색
// @Accept json
// @Produce json
// @Router /api/find-similar [post]
func (h *Handler) FindSimilarDocsById(c *gin.Context) {
	docsId := c.Query("noteId")
	if docsId == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "docsId is required"})
		return
	}

	ids, err := h.es.FindSimilarDocsById(c.Request.Context(), docsId)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"status": "success",
		"ids":    ids,
	})
}
