package com.grafkom.viewing3d;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Path2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** Main desktop application for the 3D Viewing Interactive Visualizer. */
public final class ThreeDViewing extends JFrame {
    private static final Color APP_BG = new Color(10, 16, 29);
    private static final Color PANEL_BG = new Color(17, 25, 42);
    private static final Color TEXT = new Color(234, 241, 250);
    private static final Color MUTED = new Color(145, 160, 183);
    private static final Color ACCENT = new Color(89, 211, 194);

    private final Camera3D camera = new Camera3D(6.6f, 38.0f, 27.0f);
    private final PerspectiveProjection perspective = new PerspectiveProjection(52.0f, 1.5f, 0.1f, 80.0f);
    private final StereoAnaglyphRenderer stereo = new StereoAnaglyphRenderer();
    private final ProjectionTaxonomyManager.ProjectionType[] projectionTypes =
            ProjectionTaxonomyManager.ProjectionType.values();
    private final List<JRadioButton> projectionButtons = new ArrayList<>();
    private final JLabel modeValue = new JLabel();
    private final JLabel cameraValue = new JLabel();
    private final JLabel ipdValue = new JLabel();
    private final JLabel fovValue = new JLabel();
    private final CanvasPanel canvas = new CanvasPanel();
    private JRadioButton stereoToggle;
    private JSlider ipdSlider;
    private JSlider fovSlider;

    private ProjectionTaxonomyManager.ProjectionType projectionType =
            ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3POINT;

    public ThreeDViewing() {
        super("3D Viewing Interactive Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 520));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1320, Math.max(900, screen.width - 32)),
                Math.min(850, Math.max(520, screen.height - 64)));
        setLocationRelativeTo(null);
        getContentPane().setBackground(APP_BG);
        getContentPane().setLayout(new BorderLayout(0, 0));

        getContentPane().add(buildHeader(), BorderLayout.NORTH);
        getContentPane().add(canvas, BorderLayout.CENTER);
        JScrollPane controls = new JScrollPane(buildControlPanel(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controls.setBorder(null);
        controls.getViewport().setBackground(PANEL_BG);
        controls.setPreferredSize(new Dimension(300, 100));
        getContentPane().add(controls, BorderLayout.EAST);
        getContentPane().add(buildFooter(), BorderLayout.SOUTH);

        installKeyboardControls();
        refreshControls();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(APP_BG);
        header.setBorder(new EmptyBorder(14, 20, 10, 20));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("3D VIEWING LAB");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        JLabel subtitle = new JLabel("Camera • projection • stereoscopic depth");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        JLabel week = new JLabel("GRAFIKA KOMPUTER  /  PERTEMUAN 10");
        week.setForeground(ACCENT);
        week.setFont(new Font("Segoe UI", Font.BOLD, 11));
        week.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(week, BorderLayout.EAST);
        return header;
    }

    private JPanel buildControlPanel() {
        JPanel side = new JPanel();
        side.setBackground(PANEL_BG);
        side.setPreferredSize(new Dimension(286, 690));
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel modeHeading = sectionHeading("PROJECTION MODE");
        side.add(modeHeading);
        side.add(gap(10));

        ButtonGroup group = new ButtonGroup();
        for (ProjectionTaxonomyManager.ProjectionType type : projectionTypes) {
            JRadioButton button = new JRadioButton(type.getKey() + "   " + type.getLabel());
            button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            button.setForeground(TEXT);
            button.setBackground(PANEL_BG);
            button.setFocusPainted(false);
            button.setBorder(new EmptyBorder(6, 4, 6, 4));
            button.addActionListener(event -> setProjectionType(type));
            group.add(button);
            projectionButtons.add(button);
            side.add(button);
        }

        side.add(gap(16));
        side.add(divider());
        side.add(gap(14));
        side.add(sectionHeading("STEREO VIEW"));
        side.add(gap(8));

        stereoToggle = new JRadioButton("Enable red-cyan anaglyph");
        stereoToggle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stereoToggle.setForeground(TEXT);
        stereoToggle.setBackground(PANEL_BG);
        stereoToggle.setFocusPainted(false);
        stereoToggle.addActionListener(event -> {
            setStereoEnabled(stereoToggle.isSelected());
        });
        side.add(stereoToggle);

        JPanel ipdLabels = new JPanel(new BorderLayout());
        ipdLabels.setOpaque(false);
        JLabel ipdTitle = smallLabel("Interpupillary distance");
        ipdValue.setForeground(ACCENT);
        ipdValue.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ipdLabels.add(ipdTitle, BorderLayout.WEST);
        ipdLabels.add(ipdValue, BorderLayout.EAST);
        side.add(gap(8));
        side.add(ipdLabels);
        ipdSlider = new JSlider(0, 40, 18);
        styleSlider(ipdSlider);
        ipdSlider.addChangeListener(event -> {
            stereo.setIpd(ipdSlider.getValue() / 100.0f);
            refreshLabels();
            canvas.repaint();
        });
        side.add(ipdSlider);

        side.add(gap(16));
        side.add(divider());
        side.add(gap(14));
        side.add(sectionHeading("PERSPECTIVE FRUSTUM"));
        side.add(gap(8));
        JPanel fovLabels = new JPanel(new BorderLayout());
        fovLabels.setOpaque(false);
        fovLabels.add(smallLabel("Vertical field of view"), BorderLayout.WEST);
        fovValue.setForeground(ACCENT);
        fovValue.setFont(new Font("Segoe UI", Font.BOLD, 11));
        fovLabels.add(fovValue, BorderLayout.EAST);
        side.add(fovLabels);
        fovSlider = new JSlider(28, 82, 52);
        styleSlider(fovSlider);
        fovSlider.addChangeListener(event -> {
            perspective.setFovDegree(fovSlider.getValue());
            refreshLabels();
            canvas.repaint();
        });
        side.add(fovSlider);

        side.add(gap(18));
        side.add(divider());
        side.add(gap(14));
        side.add(sectionHeading("LIVE CAMERA"));
        side.add(gap(9));
        modeValue.setForeground(TEXT);
        modeValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cameraValue.setForeground(MUTED);
        cameraValue.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        side.add(modeValue);
        side.add(gap(5));
        side.add(cameraValue);
        side.add(gap(5));
        JLabel hint = smallLabel("Drag to orbit  •  scroll to zoom");
        side.add(hint);
        side.add(javax.swing.Box.createVerticalGlue());
        return side;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(APP_BG);
        footer.setBorder(new EmptyBorder(8, 20, 10, 20));
        JLabel left = new JLabel("MOUSE DRAG  Orbit     •     SCROLL  Zoom     •     SPACE  Anaglyph");
        left.setForeground(MUTED);
        left.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel right = new JLabel("JAVA 17+  /  SOFTWARE RENDERER");
        right.setForeground(new Color(105, 121, 146));
        right.setFont(new Font("Segoe UI", Font.BOLD, 10));
        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void installKeyboardControls() {
        for (ProjectionTaxonomyManager.ProjectionType type : projectionTypes) {
            bindKey(Integer.toString(type.getKey()), "projection-" + type.getKey(),
                    () -> setProjectionType(type));
        }
        bindKey("SPACE", "toggle-stereo", () -> {
            setStereoEnabled(!stereo.isEnabled());
        });
        bindKey("R", "reset-camera", () -> {
            camera.reset();
            ProjectionTaxonomyManager.applyProjection(projectionType, camera);
            refreshControls();
            canvas.repaint();
        });
        bindKey("OPEN_BRACKET", "decrease-ipd", () -> adjustIpd(-0.02f));
        bindKey("CLOSE_BRACKET", "increase-ipd", () -> adjustIpd(0.02f));
        bindKey("MINUS", "decrease-fov", () -> adjustFov(-2));
        bindKey("EQUALS", "increase-fov", () -> adjustFov(2));
        bindKey("shift EQUALS", "increase-fov-plus", () -> adjustFov(2));
    }

    private void bindKey(String key, String name, Runnable action) {
        canvas.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        canvas.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    private void setProjectionType(ProjectionTaxonomyManager.ProjectionType type) {
        projectionType = type;
        ProjectionTaxonomyManager.applyProjection(type, camera);
        refreshControls();
        canvas.repaint();
    }

    private void adjustIpd(float delta) {
        stereo.setIpd(stereo.getIpd() + delta);
        if (ipdSlider != null) ipdSlider.setValue(Math.round(stereo.getIpd() * 100.0f));
        refreshLabels();
        canvas.repaint();
    }

    private void adjustFov(int delta) {
        perspective.setFovDegree(perspective.getFovDegree() + delta);
        if (fovSlider != null) fovSlider.setValue(Math.round(perspective.getFovDegree()));
        refreshLabels();
        canvas.repaint();
    }

    private void setStereoEnabled(boolean enabled) {
        stereo.setEnabled(enabled);
        if (stereoToggle != null) stereoToggle.setSelected(enabled);
        canvas.repaint();
    }

    private void refreshControls() {
        for (int i = 0; i < projectionTypes.length; i++) {
            projectionButtons.get(i).setSelected(projectionTypes[i] == projectionType);
        }
        modeValue.setText(projectionType.getLabel());
        cameraValue.setText(String.format("AZ %03.0f°   EL %02.0f°   R %.1f",
                camera.getAzimuthDegrees(), camera.getElevationDegrees(), camera.getRadius()));
        refreshLabels();
    }

    private void refreshLabels() {
        ipdValue.setText(String.format("%.2f u", stereo.getIpd()));
        fovValue.setText(String.format("%.0f°", perspective.getFovDegree()));
        if (cameraValue != null) {
            cameraValue.setText(String.format("AZ %03.0f°   EL %02.0f°   R %.1f",
                    camera.getAzimuthDegrees(), camera.getElevationDegrees(), camera.getRadius()));
        }
    }

    private static JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        return label;
    }

    private static JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return label;
    }

    private static JPanel gap(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, height));
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return spacer;
    }

    private static JPanel divider() {
        JPanel line = new JPanel();
        line.setBackground(new Color(39, 51, 73));
        line.setPreferredSize(new Dimension(1, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    private static void styleSlider(JSlider slider) {
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setForeground(ACCENT);
        slider.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    private final class CanvasPanel extends JPanel {
        private final Mesh3D cube = Mesh3D.createColorCube(1.35f);
        private float rotation = 0.42f;
        private PointDrag drag;

        CanvasPanel() {
            setBackground(APP_BG);
            setMinimumSize(new Dimension(580, 420));
            setBorder(new EmptyBorder(0, 20, 0, 14));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    drag = new PointDrag(event.getX(), event.getY());
                    requestFocusInWindow();
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent event) {
                    if (drag != null) {
                        camera.orbit(event.getX() - drag.x, event.getY() - drag.y);
                        drag = new PointDrag(event.getX(), event.getY());
                        refreshControls();
                        repaint();
                    }
                }
            });
            addMouseWheelListener((MouseWheelEvent event) -> {
                camera.zoom((float) event.getPreciseWheelRotation());
                refreshControls();
                repaint();
            });
            new Timer(32, event -> {
                rotation += 0.008f;
                repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int width = getWidth();
            int height = getHeight();
            if (width < 2 || height < 2) return;
            BufferedImage frame = stereo.render(width, height,
                    (eyeGraphics, eyeOffset, leftEye) -> drawScene(eyeGraphics, width, height, eyeOffset));
            graphics.drawImage(frame, 0, 0, null);
        }

        private void drawScene(Graphics2D source, int width, int height, float eyeOffset) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setPaint(new GradientPaint(0, 0, new Color(10, 20, 36),
                    width, height, new Color(16, 26, 43)));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(98, 132, 165, 17));
            int gridStep = 34;
            for (int x = 18; x < width; x += gridStep) g.drawLine(x, 0, x, height);
            for (int y = 16; y < height; y += gridStep) g.drawLine(0, y, width, y);

            float[] viewMatrix = camera.calculateLookAtMatrix(eyeOffset);
            drawWorldGuides(g, width, height, viewMatrix, eyeOffset);
            drawCube(g, width, height, viewMatrix, eyeOffset);
            drawViewportLabels(g, width, height);
            g.dispose();
        }

        private void drawWorldGuides(Graphics2D g, int width, int height, float[] viewMatrix, float eyeOffset) {
            g.setStroke(new BasicStroke(1.0f));
            for (int i = -6; i <= 6; i++) {
                float coordinate = i * 0.5f;
                Color gridColor = i == 0 ? new Color(133, 164, 191, 48) : new Color(118, 146, 173, 23);
                g.setColor(gridColor);
                drawWorldLine(g, new Vec3(coordinate, -1.7f, -3.0f),
                        new Vec3(coordinate, -1.7f, 3.0f), width, height, viewMatrix, eyeOffset);
                drawWorldLine(g, new Vec3(-3.0f, -1.7f, coordinate),
                        new Vec3(3.0f, -1.7f, coordinate), width, height, viewMatrix, eyeOffset);
            }
            g.setStroke(new BasicStroke(1.7f));
            drawAxis(g, new Vec3(2.0f, 0, 0), new Color(248, 112, 126, 170), width, height, viewMatrix, eyeOffset);
            drawAxis(g, new Vec3(0, 2.0f, 0), new Color(112, 224, 158, 170), width, height, viewMatrix, eyeOffset);
            drawAxis(g, new Vec3(0, 0, 2.0f), new Color(95, 171, 244, 170), width, height, viewMatrix, eyeOffset);
        }

        private void drawAxis(Graphics2D g, Vec3 end, Color color, int width, int height,
                              float[] viewMatrix, float eyeOffset) {
            g.setColor(color);
            drawWorldLine(g, new Vec3(0, 0, 0), end, width, height, viewMatrix, eyeOffset);
        }

        private void drawWorldLine(Graphics2D g, Vec3 start, Vec3 end, int width, int height,
                                   float[] viewMatrix, float eyeOffset) {
            Vec3 viewStart = camera.transformToView(start, viewMatrix);
            Vec3 viewEnd = camera.transformToView(end, viewMatrix);
            PerspectiveProjection.ProjectionPoint a = ProjectionTaxonomyManager.project(
                    projectionType, viewStart, perspective, width, height, eyeOffset, stereo.getConvergenceDistance());
            PerspectiveProjection.ProjectionPoint b = ProjectionTaxonomyManager.project(
                    projectionType, viewEnd, perspective, width, height, eyeOffset, stereo.getConvergenceDistance());
            if (a != null && b != null) {
                g.drawLine(Math.round(a.x), Math.round(a.y), Math.round(b.x), Math.round(b.y));
            }
        }

        private void drawCube(Graphics2D g, int width, int height, float[] viewMatrix, float eyeOffset) {
            Vec3[] transformed = new Vec3[cube.vertices().length];
            float secondRotation = rotation * 0.67f;
            for (int i = 0; i < transformed.length; i++) {
                Vec3 model = cube.vertices()[i].rotateY(rotation).rotateX(secondRotation);
                transformed[i] = camera.transformToView(model, viewMatrix);
            }

            List<FaceToDraw> drawOrder = new ArrayList<>();
            for (Mesh3D.Face face : cube.faces()) {
                float depth = 0;
                boolean inFront = false;
                for (int index : face.indices) {
                    depth += -transformed[index].z;
                    inFront |= -transformed[index].z > perspective.getZNear();
                }
                if (inFront) drawOrder.add(new FaceToDraw(face, depth / face.indices.length));
            }
            drawOrder.sort(Comparator.comparingDouble((FaceToDraw face) -> face.depth).reversed());

            for (FaceToDraw item : drawOrder) {
                Path2D path = new Path2D.Float();
                boolean valid = true;
                for (int i = 0; i < item.face.indices.length; i++) {
                    Vec3 view = transformed[item.face.indices[i]];
                    PerspectiveProjection.ProjectionPoint point = ProjectionTaxonomyManager.project(
                            projectionType, view, perspective, width, height, eyeOffset,
                            stereo.getConvergenceDistance());
                    if (point == null) {
                        valid = false;
                        break;
                    }
                    if (i == 0) path.moveTo(point.x, point.y);
                    else path.lineTo(point.x, point.y);
                }
                if (!valid) continue;
                path.closePath();
                float shade = Math.max(0.66f, Math.min(1.0f, 1.08f - item.depth * 0.025f));
                g.setColor(shade(item.face.color, shade));
                g.fill(path);
                g.setColor(new Color(235, 248, 255, 205));
                g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(path);
            }
        }

        private void drawViewportLabels(Graphics2D g, int width, int height) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g.setColor(new Color(196, 213, 232, 180));
            g.drawString("WORLD SPACE", 18, 24);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.setColor(new Color(143, 162, 184, 155));
            g.drawString("XYZ  /  CAMERA VIEW", 18, 41);

            String mode = projectionType.getLabel().toUpperCase();
            int labelWidth = g.getFontMetrics().stringWidth(mode);
            g.setColor(new Color(13, 23, 39, 205));
            g.fillRoundRect(width - labelWidth - 42, 17, labelWidth + 24, 28, 12, 12);
            g.setColor(ACCENT);
            g.drawString(mode, width - labelWidth - 30, 35);

            if (stereo.isEnabled()) {
                g.setColor(new Color(255, 105, 128));
                g.fillOval(width - 17, 58, 7, 7);
                g.setColor(new Color(94, 220, 227));
                g.fillOval(width - 17, 69, 7, 7);
                g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g.setColor(new Color(211, 222, 238, 175));
                g.drawString("STEREO ACTIVE", width - 103, 68);
            }
        }

        private Color shade(Color color, float factor) {
            return new Color(Math.min(255, Math.round(color.getRed() * factor)),
                    Math.min(255, Math.round(color.getGreen() * factor)),
                    Math.min(255, Math.round(color.getBlue() * factor)));
        }
    }

    private static final class PointDrag {
        final int x;
        final int y;

        PointDrag(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class FaceToDraw {
        final Mesh3D.Face face;
        final float depth;

        FaceToDraw(Mesh3D.Face face, float depth) {
            this.face = face;
            this.depth = depth;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing's default look and feel remains usable if the system one is unavailable.
            }
            ThreeDViewing app = new ThreeDViewing();
            app.setVisible(true);
        });
    }
}
