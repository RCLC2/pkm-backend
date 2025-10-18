package handlers

import (
	"graph/services"
	"net/http"

	"github.com/gin-gonic/gin"
)

type GraphHandler struct {
	service *services.GraphService
}

func NewGraphHandler(gs *services.GraphService) *GraphHandler {
	return &GraphHandler{service: gs}
}

// SetParaCategory
// @Summary 문서에 PARA 카테고리 설정
// @Description 특정 문서를 PARA(Projects, Areas, Resources, Archives) 시스템의 카테고리로 지정합니다.
// @Tags Document
// @Accept json
// @Produce json
// @Param documentId path string true "카테고리를 설정할 문서 ID"
// @Param request body object{category=string} true "Category (e.g., Project, Area)"
// @Router /documents/{documentId}/para [post]
func (h *GraphHandler) SetParaCategory(c *gin.Context) {
	docID := c.Param("documentId")
	var req struct {
		Category string `json:"category" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.service.SetParaCategory(c.Request.Context(), docID, req.Category); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to set PARA category"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

// ConvertToZettelkasten
// @Summary 문서를 제텔카스텐 형식으로 변환 (PARA 카테고리 제거)
// @Description 특정 문서에서 기존에 설정된 PARA 카테고리 정보를 제거하여 제텔카스텐(Zettelkasten) 방식으로 전환합니다.
// @Tags Document
// @Produce json
// @Param documentId path string true "변환할 문서 ID"
// @Router /documents/{documentId}/zettelkasten [post]
func (h *GraphHandler) ConvertToZettelkasten(c *gin.Context) {
	docID := c.Param("documentId")
	if err := h.service.ConvertToZettelkasten(c.Request.Context(), docID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to convert to Zettelkasten"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

// ConfirmGraphConnection
// @Summary 그래프 연결 확정
// @Description 제안된(pending) 또는 편집된(edited) 문서 간의 그래프 연결을 확정(confirmed) 상태로 변경합니다.
// @Tags Graph Connection
// @Accept json
// @Produce json
// @Param request body object{sourceId=string, targetId=string} true "연결을 확정할 Source 문서 ID 및 Target 문서 ID"
// @Router /connections/confirm [post]
func (h *GraphHandler) ConfirmGraphConnection(c *gin.Context) {
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
func (h *GraphHandler) EditGraphConnection(c *gin.Context) {
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

// GetWorkspaceGraph
// @Summary 워크스페이스 그래프 구조 조회
// @Description 특정 워크스페이스 내 모든 문서 노드와 그 연결 관계(엣지)를 조회합니다.
// @Tags Workspace Graph
// @Produce json
// @Param workspaceId path string true "조회할 워크스페이스 ID"
// @Router /workspaces/{workspaceId}/graph [get]
func (h *GraphHandler) GetWorkspaceGraph(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	response, err := h.service.GetWorkspaceGraphResponse(c.Request.Context(), workspaceID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch workspace graph"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "data": response})
}

// AutoConnectWorkspace
// @Summary 워크스페이스 문서 자동 연결 (PARA -> Zettelkasten 변환 단계)
// @Description 워크스페이스의 모든 문서를 분석하여 유사한 문서 쌍을 찾아 'pending' 상태의 백링크를 자동으로 생성합니다.
// @Tags Workspace Graph
// @Produce json
// @Param workspaceId path string true "자동 연결을 수행할 워크스페이스 ID"
// @Router /workspaces/{workspaceId}/autoconnect [post]
func (h *GraphHandler) AutoConnectWorkspace(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	connections, err := h.service.AutoConnectWorkspace(c, workspaceID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to auto-connect workspace nodes"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "connections": connections})
}

// ConfirmAllConnections
// @Summary 워크스페이스 내 모든 '미완료' 연결 확정
// @Description 특정 워크스페이스 내의 모든 'pending' 또는 'edit' 상태의 그래프 연결을 'confirmed' 상태로 일괄 변경합니다.
// @Tags Workspace Graph
// @Produce json
// @Param workspaceId path string true "연결을 일괄 확정할 워크스페이스 ID"
// @Router /workspaces/{workspaceId}/confirm-all [post]
func (h *GraphHandler) ConfirmAllConnections(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	if err := h.service.ConfirmAllConnections(c.Request.Context(), workspaceID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to confirm all connections"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}
