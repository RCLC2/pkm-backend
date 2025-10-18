package handlers

import (
	"fmt"
	"graph/services"
	"net/http"

	"github.com/gin-gonic/gin"
)

type WorkspaceGraphHandler struct {
	ws *services.WorkspaceService
	gs *services.GraphService
}

func NewWorkspaceGraphHandler(gs *services.GraphService, ws *services.WorkspaceService) *WorkspaceGraphHandler {
	return &WorkspaceGraphHandler{
		ws: ws,
		gs: gs,
	}
}

// CreateWorkspace
// @Summary 워크스페이스 생성
// @Description 새로운 워크스페이스를 생성합니다. 생성자는 X-User-ID 헤더에서 가져옵니다.
// @Tags Workspace
// @Produce json
// @Param name body string true "워크스페이스 이름"
// @Param type body string true "워크스페이스 타입"
// @Router /api/workspaces [post]
func (h *WorkspaceGraphHandler) CreateWorkspace(c *gin.Context) {
	var req struct {
		Name string `json:"name" binding:"required"`
		Type string `json:"type" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "X-User-ID header is missing"})
		return
	}

	workspaceID, err := h.ws.CreateWorkspace(c.Request.Context(), req.Name, req.Type, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create workspace"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "workspaceId": workspaceID})
}

// UpdateWorkspace
// @Summary 워크스페이스 수정
// @Description 워크스페이스 이름이나 타입을 수정합니다.
// @Tags Workspace
// @Produce json
// @Param workspaceId path string true "수정할 워크스페이스 ID"
// @Param name body string false "새 워크스페이스 이름"
// @Param type body string false "새 워크스페이스 타입"
// @Router /api/workspaces/{workspaceId} [put]
func (h *WorkspaceGraphHandler) UpdateWorkspace(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "X-User-ID header is missing"})
		return
	}

	workspaceID := c.Param("workspaceId")
	var req struct {
		Name *string `json:"name"`
		Type *string `json:"type"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	msg, err := h.ws.UpdateWorkspace(c.Request.Context(), workspaceID, userID, req.Name, req.Type)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to update workspace: %s", err.Error())})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "message": msg})
}

// DeleteWorkspace
// @Summary 워크스페이스 삭제
// @Description 워크스페이스와 해당 워크스페이스 내 모든 그래프 데이터를 삭제합니다.
// @Tags Workspace
// @Produce json
// @Param workspaceId path string true "삭제할 워크스페이스 ID"
// @Router /api/workspaces/{workspaceId} [delete]
func (h *WorkspaceGraphHandler) DeleteWorkspace(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "X-User-ID header is missing"})
		return
	}

	if err := h.ws.DeleteWorkspace(c.Request.Context(), workspaceID, userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to delete workspace: %s", err.Error())})
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
// @Router /api/workspaces/{workspaceId}/graph [get]
func (h *WorkspaceGraphHandler) GetWorkspaceGraph(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	response, err := h.gs.GetWorkspaceGraphResponse(c.Request.Context(), workspaceID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch workspace graph"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "data": response})
}

// ChangeWorkspaceStyle
// @Summary 워크스페이스 스타일 변경
// @Description 워크스페이스의 스타일을 변경하고, 해당 스타일에 맞는 그래프 연결 작업을 비동기로 수행합니다.
// @Tags Workspace
// @Produce json
// @Param workspaceId path string true "변경할 워크스페이스 ID"
// @Param newStyle body string true "새 워크스페이스 스타일 (zettel, generic, para)"
// @Router /api/workspaces/{workspaceId}/style [post]
func (h *WorkspaceGraphHandler) ChangeWorkspaceStyle(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "X-User-ID header is missing"})
		return
	}

	var req struct {
		NewStyle string `json:"newStyle" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	msg, err := h.ws.ChangeWorkspaceStyle(c.Request.Context(), workspaceID, userID, req.NewStyle)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to change workspace style: %s", err.Error())})
		return
	}

	c.JSON(http.StatusOK, gin.H{"status": "success", "message": msg})
}

// ConfirmAllConnections
// @Summary 워크스페이스 내 모든 '미완료' 연결 확정
// @Description 특정 워크스페이스 내의 모든 'pending' 또는 'edit' 상태의 그래프 연결을 'confirmed' 상태로 일괄 변경합니다.
// @Tags Workspace Graph
// @Produce json
// @Param workspaceId path string true "연결을 일괄 확정할 워크스페이스 ID"
// @Router /api/workspaces/{workspaceId}/confirm-all [post]
func (h *WorkspaceGraphHandler) ConfirmAllConnections(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	if err := h.gs.ConfirmAllConnections(c.Request.Context(), workspaceID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to confirm all connections"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

// GetWorkspaceType
// @Summary 워크스페이스 타입 조회
// @Description 특정 워크스페이스가 존재할 경우 해당 워크스페이스의 타입을 반환합니다.
// @Tags Workspace
// @Produce json
// @Param workspaceId path string true "조회할 워크스페이스 ID"
// @Param X-User-ID header string true "사용자 ID"
// @Success 200 {object} map[string]string "{"status": "success", "type": "generic"}"
// @Failure 404 {object} map[string]string "{"error": "Workspace not found"}"
// @Failure 500 {object} map[string]string "{"error": "..."}"
// @Router /api/workspaces/{workspaceId}/type [get]
func (h *WorkspaceGraphHandler) GetWorkspaceType(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "X-User-ID header is missing"})
		return
	}

	workspaceType, exists, err := h.ws.CheckWorkspace(c.Request.Context(), workspaceID, userID)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to check workspace: %s", err.Error())})
		return
	}

	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Workspace not found or unauthorized"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"status": "success", "type": workspaceType})
}
