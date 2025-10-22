package handlers

import (
	"fmt"
	"graph/services"
	"net/http"

	"github.com/gin-gonic/gin"
)

type GraphConnectionHandler struct {
	service *services.GraphService
}

func NewGraphConnectionHandler(gs *services.GraphService) *GraphConnectionHandler {
	return &GraphConnectionHandler{service: gs}
}

// ConfirmGraphConnection
// @Summary 그래프 연결 확정
// @Description 제안된(pending) 또는 편집된(edited) 문서 간의 그래프 연결을 확정(confirmed) 상태로 변경합니다.
// @Tags Graph Connection
// @Accept json
// @Produce json
// @Param request body object{sourceId=string, targetId=string} true "연결을 확정할 Source 문서 ID 및 Target 문서 ID"
// @Router /connections/confirm [post]
func (h *GraphConnectionHandler) ConfirmGraphConnection(c *gin.Context) {
	var req struct {
		SourceID string `json:"sourceId" binding:"required"`
		TargetID string `json:"targetId" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.service.ConfirmGraphConnection(c.Request.Context(), req.SourceID, req.TargetID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to confirm connection"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

// EditGraphConnection
// @Summary 그래프 연결 편집 상태로 설정
// @Description 사용자가 문서 간의 연결을 수동으로 편집했음을 표시하기 위해 연결 상태를 'edited'로 변경합니다.
// @Tags Graph Connection
// @Accept json
// @Produce json
// @Param request body object{sourceId=string, targetId=string} true "편집 상태로 설정할 Source 문서 ID 및 Target 문서 ID"
// @Router /connections/edit [post]
func (h *GraphConnectionHandler) EditGraphConnection(c *gin.Context) {
	var req struct {
		SourceID string `json:"sourceId" binding:"required"`
		TargetID string `json:"targetId" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.service.EditGraphConnection(c.Request.Context(), req.SourceID, req.TargetID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to edit connection"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

// DeleteNoteGraph
// @Summary 노트 삭제 이벤트 처리
// @Description 외부 노트 서비스에서 노트 삭제 시 관련 그래프 연결을 모두 삭제합니다.
// @Tags Graph Connection
// @Accept json
// @Produce json
// @Param request body object{noteId=string} true "삭제된 노트 ID"
// @Router /connections/note-deleted [post]
func (h *GraphConnectionHandler) DeleteNoteGraph(c *gin.Context) {
	var req struct {
		NoteID string `json:"noteId" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if err := h.service.NoteDeleted(c.Request.Context(), req.NoteID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("failed to delete graph connections: %v", err)})
		return
	}

	c.JSON(http.StatusOK, gin.H{"status": "success"})
}
