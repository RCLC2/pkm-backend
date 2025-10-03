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

func (h *GraphHandler) ConvertToZettelkasten(c *gin.Context) {
	docID := c.Param("documentId")
	if err := h.service.ConvertToZettelkasten(c.Request.Context(), docID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to convert to Zettelkasten"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}

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

func (h *GraphHandler) GetWorkspaceGraph(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	response, err := h.service.GetWorkspaceGraphResponse(c.Request.Context(), workspaceID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch workspace graph"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "data": response})
}

func (h *GraphHandler) AutoConnectWorkspace(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	connections, err := h.service.AutoConnectWorkspace(c, workspaceID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to auto-connect workspace nodes"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success", "connections": connections})
}

func (h *GraphHandler) ConfirmAllConnections(c *gin.Context) {
	workspaceID := c.Param("workspaceId")
	if err := h.service.ConfirmAllConnections(c.Request.Context(), workspaceID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to confirm all connections"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "success"})
}
