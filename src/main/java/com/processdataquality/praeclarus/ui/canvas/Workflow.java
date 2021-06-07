/*
 * Copyright (c) 2021 Queensland University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.processdataquality.praeclarus.ui.canvas;

import com.processdataquality.praeclarus.ui.component.PipelinePanel;
import com.processdataquality.praeclarus.ui.component.VertexLabelDialog;
import com.processdataquality.praeclarus.workspace.node.Node;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 19/5/21
 */
public class Workflow implements CanvasEventListener {

    private enum State { VERTEX_DRAG, ARC_DRAW, NONE }

    private final PipelinePanel _parent;
    private final Context2D _ctx;
    private final Set<Vertex> _vertices = new HashSet<>();
    private final Set<Connector> _connectors = new HashSet<>();

    private ActiveLine activeLine;
    private CanvasPrimitive selected;
    private State state = State.NONE;
    private boolean _loading = false;


    public Workflow(PipelinePanel parent, Context2D context) {
        _parent = parent;
        _ctx = context;
    }

    @Override
    public void mouseDown(double x, double y) {
        Port port = getPortAt(x, y);
        if (port != null && port.isOutput()) {
            activeLine = new ActiveLine(x, y);
            state = State.ARC_DRAW;
        }
        else {
            Vertex vertex = getVertexAt(x, y);
            if (vertex != null) {
                selected = vertex;
                vertex.setDragOffset(x, y);
                state = State.VERTEX_DRAG;
            }
        }
    }

    @Override
    public void mouseMove(double x, double y) {
        if (state == State.NONE) return;

        if (state == State.ARC_DRAW) {
            render();
            activeLine.lineTo(_ctx, x, y);
        }
        else if (state == State.VERTEX_DRAG) {
            ((Vertex) selected).moveTo(x, y);
            render();
        }
    }

    @Override
    public void mouseUp(double x, double y) {
        if (state == State.NONE) return;

        if (state == State.ARC_DRAW) {
            Point start = activeLine.getStart();
            Port source = getPortAt(start.x, start.y);
            Port target = getPortAt(x, y);
            if (source != null && target != null) {
                if (source.isOutput() && target.isInput()) {
                    addConnector(new Connector(source, target));
                }
                else {
                    Notification.show("Output port cannot be the target of a connection");
                }
            }
        }

        state = State.NONE;     // if dragging nothing more to do
        render();
    }

    @Override
    public void mouseClick(double x, double y) {
        selected = setSelected(x, y);
        _parent.changedSelected(getSelectedNode());
        render();
    }

    @Override
    public void mouseDblClick(double x, double y) {
        mouseClick(x, y);
        Vertex selectedVertex = getSelectedVertex();
        if (selectedVertex != null) {
            new VertexLabelDialog(this, selectedVertex).open();
        }
    }

    @Override
    public void fileLoaded(String jsonStr) {
        WorkflowLoader loader = new WorkflowLoader(this, _parent.getWorkspace());
        try {
            loader.load(jsonStr);
        }
        catch (JSONException je) {
            Notification.show("Failed to load file: " + je.getMessage());
        }
    }


    public void clear() {
        _vertices.clear();
        _connectors.clear();
        render();
    }


    public void setLoading(boolean b) {
        _loading = b;
        if (! _loading) render();
    }


    public boolean hasContent() {
        return ! _vertices.isEmpty();
    }


    public CanvasPrimitive getSelected() { return selected; }

    public void setSelected(CanvasPrimitive primitive) {
        selected = primitive;
        render();
    }

    public Node getSelectedNode() {
        Vertex vertex = getSelectedVertex();
        return vertex != null ? vertex.getNode() : null;
    }


    public Vertex getSelectedVertex() {
        return selected instanceof Vertex ? ((Vertex) selected) : null;
    }


    public void setSelectedNode(Node node) {
        for (Vertex vertex : _vertices) {
            if (vertex.getNode().equals(node)) {
                CanvasPrimitive prevSelected = selected;
                setSelected(vertex);
                if (selected != prevSelected) {
                    _parent.changedSelected(node);
                }
                break;
            }
        }
    }


    public void removeSelected() {
        if (selected instanceof Vertex) {
            removeVertex((Vertex) selected);
            selected = null;
        }
        else if (selected instanceof Connector) {
            removeConnector((Connector) selected);
            selected = null;
        }
    }

    
    public void addVertex(Vertex vertex) {
        _vertices.add(vertex);
        selected = vertex;
        render();
    }


    public void addVertex(Node node) {
        Point p = getSuitableInsertPoint();
        addVertex(new Vertex(p.x, p.y, node));
    }


    public void removeVertex(Vertex vertex) {
        if (vertex != null) {
            _vertices.remove(vertex);
            removeConnectors(vertex);
            render();
        }
    }


    public void addConnector(Connector c) {
        _connectors.add(c);
        Node source = c.getSource().getNode();
        Node target = c.getTarget().getNode();
        _parent.getWorkspace().connect(source, target);
        render();
    }

    public boolean removeConnector(Connector c) {
        boolean success = _connectors.remove(c);
        if (success) {
            Node previous = c.getSource().getNode();
            Node next = c.getTarget().getNode();
            _parent.getWorkspace().disconnect(previous, next);
            render();
        }
        return success;
    }


    public JSONObject asJson() throws JSONException {
        JSONArray vertexArray = new JSONArray();
        for (Vertex vertex : _vertices) {
            vertexArray.put(vertex.asJson());
        }
        JSONArray connectorArray = new JSONArray();
        for (Connector connector : _connectors) {
            connectorArray.put(connector.asJson());
        }
        JSONObject json = new JSONObject();
        json.put("vertices", vertexArray);
        json.put("connectors", connectorArray);
        return json;
    }
    

    private void removeConnectors(Vertex vertex) {
        Set<Connector> removeSet = new HashSet<>();
        for (Connector c : _connectors) {
            if (c.connects(vertex)) {
                removeSet.add(c);
            }
        }
        for (Connector c : removeSet) {
            removeConnector(c);
        }
        render();
    }


    private Port getPortAt(double x, double y) {
        for (Vertex vertex : _vertices) {
            Port port = vertex.getPortAt(x, y);
             if (port != null) {
                 return port;
             }
        }
        return null;
    }

    private Vertex getVertexAt(double x, double y) {
        for (Vertex vertex : _vertices) {
            if (vertex.contains(x, y)) {
                return vertex;
            }
        }
        return null;
    }


    public void render() {
        if (_loading) return;                 // don't render while loading from file
        _ctx.clear();
        for (Vertex vertex : _vertices) {
            vertex.render(_ctx, selected);
        }
        for (Connector connector : _connectors) {
            connector.render(_ctx, selected);
        }
    }


    private CanvasPrimitive setSelected(double x, double y) {
        for (Vertex vertex : _vertices) {
            if (vertex.contains(x, y)) {
                return vertex;
            }
        }
        for (Connector connector : _connectors) {
            if (connector.contains(x, y)) {
                return connector;
            }
        }
        return null;
    }


    private Point getSuitableInsertPoint() {
        double x = 50;
        double sepSpace = 100;
        double y = 50;
        boolean overlap;
        do {
            overlap = false;
            for (Vertex vertex : _vertices) {
                if (! vertex.contains(x + 10, y + 10)) continue;

                overlap = true;
                x = x + Vertex.WIDTH + sepSpace;
            }
        } while (overlap);

        return new Point(x, y);
    }

}