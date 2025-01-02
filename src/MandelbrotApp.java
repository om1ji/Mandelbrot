import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.awt.geom.Rectangle2D;
import java.util.Stack;

public class MandelbrotApp extends JFrame {
    private BufferedImage image;
    private double zoomFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();
    private Stack<BufferedImage> undoStack = new Stack<>();
    private Point startPoint;
    private MandelbrotCalculator calculator;
    private float colorOffset = 0.0f;
    private int maxIter = 1000;

    public MandelbrotApp() {
        setTitle("Mandelbrot Set");
        setSize(1000, 1000);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        calculator = new MandelbrotCalculator(maxIter);

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                if (!selectionRect.isEmpty()) {
                    g.setColor(Color.RED);
                    ((Graphics2D) g).draw(selectionRect);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(800, 800));
        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                if (!SwingUtilities.isRightMouseButton(e)) {
                    selectionRect.setRect(startPoint.x, startPoint.y, 0, 0);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e) && !selectionRect.isEmpty()) {
                    double centerX = selectionRect.getCenterX() / imagePanel.getWidth() - 0.5;
                    double centerY = selectionRect.getCenterY() / imagePanel.getHeight() - 0.5;
                    offsetX += centerX * 4.0 / zoomFactor;
                    offsetY += centerY * 4.0 / zoomFactor;
                    zoomFactor *= imagePanel.getWidth() / selectionRect.getWidth();
                    drawMandelbrot();
                    selectionRect.setRect(0, 0, 0, 0);
                }
            }
        });

        imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int dx = e.getX() - startPoint.x;
                    int dy = e.getY() - startPoint.y;
                    offsetX -= dx * 4.0 / (zoomFactor * imagePanel.getWidth());
                    offsetY -= dy * 4.0 / (zoomFactor * imagePanel.getHeight());
                    startPoint = e.getPoint();
                    drawMandelbrot();
                } else {
                    int dx = e.getX() - startPoint.x;
                    int dy = e.getY() - startPoint.y;
                    int size = Math.min(Math.abs(dx), Math.abs(dy));
                    selectionRect.setRect(
                        startPoint.x,
                        startPoint.y,
                        dx < 0 ? -size : size,
                        dy < 0 ? -size : size
                    );
                    imagePanel.repaint();
                }
            }
        });

        imagePanel.addMouseWheelListener(e -> {
            double scaleFactor = e.getPreciseWheelRotation() < 0 ? 1.1 : 0.9;
            zoomFactor *= scaleFactor;
            drawMandelbrot();
        });

        InputMap inputMap = imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = imagePanel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem saveItem = new JMenuItem("Save Image");
        saveItem.addActionListener(e -> saveImage());
        fileMenu.add(saveItem);

        JMenuItem resetItem = new JMenuItem("Reset View");
        resetItem.addActionListener(e -> resetView());
        fileMenu.add(resetItem);

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.addActionListener(e -> undo());
        fileMenu.add(undoItem);

        MandelbrotUI controlPanel = new MandelbrotUI();
        controlPanel.addColorSliderListener(e -> {
            colorOffset = controlPanel.getColorOffset() / 100.0f;
            drawMandelbrot();
        });
        controlPanel.addIterSliderListener(e -> {
            maxIter = controlPanel.getMaxIter();
            calculator = new MandelbrotCalculator(maxIter);
            drawMandelbrot();
        });
        controlPanel.addResetButtonListener(e -> resetView());
        controlPanel.addUndoButtonListener(e -> undo());

        setLayout(new BorderLayout());
        add(imagePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        drawMandelbrot();
    }

    private void drawMandelbrot() {
        if (undoStack.size() >= 100) {
            undoStack.remove(0);
        }
        undoStack.push(copyImage(image));

        int width = image.getWidth();
        int height = image.getHeight();
        double minX = -2.0 / zoomFactor + offsetX;
        double maxX = 2.0 / zoomFactor + offsetX;
        double minY = -2.0 / zoomFactor + offsetY;
        double maxY = 2.0 / zoomFactor + offsetY;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int numThreads = Runtime.getRuntime().availableProcessors();
        int rowsPerThread = height / numThreads;

        for (int i = 0; i < numThreads; i++) {
            final int startY = i * rowsPerThread;
            final int endY = (i == numThreads - 1) ? height : startY + rowsPerThread;

            executor.submit(() -> {
                for (int y = startY; y < endY; y++) {
                    double cy = minY + y * (maxY - minY) / height;
                    for (int x = 0; x < width; x++) {
                        double cx = minX + x * (maxX - minX) / width;
                        int color = calculator.calculateColor(cx, cy, colorOffset);
                        image.setRGB(x, y, color);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        repaint();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            image = undoStack.pop();
            repaint();
        }
    }

    private void resetView() {
        zoomFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        drawMandelbrot();
    }

    private void saveImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ImageIO.write(image, "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MandelbrotApp app = new MandelbrotApp();
            app.setVisible(true);
            app.requestFocusInWindow();
        });
    }
}