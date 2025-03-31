package ch.heig.gre.gui;

import ch.heig.gre.graph.GridGraph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.GridMazeSolver;
import ch.heig.gre.gui.impl.*;
import ch.heig.gre.maze.impl.MazeIO;
import ch.heig.gre.maze.impl.MazeTuner;
import ch.heig.gre.maze.impl.ShenaniganWeightFunction;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.*;

public final class MainViewController implements Initializable {
  @FXML private TitledPane generationGroup;
  @FXML private Slider gridSizeSlider;
  @FXML private CheckBox animateGen;
  @FXML private TitledPane tuneGroupAll;
  @FXML private Pane tuneGroup;
  @FXML private Slider wallRemovalSlider;
  @FXML private Slider reliefDensitySlider;
  @FXML private Slider reliefRadiusSlider;
  @FXML private CheckBox autoTune;
  @FXML private TitledPane solveGroup;
  @FXML private ChoiceBox<ChoiceItem<GridMazeSolver>> solverChoiceBox;
  @FXML private Label resultLength;
  @FXML private Label resultPerformance;
  @FXML private Pane playPauseGroup;
  @FXML private Slider delaySlider;
  @FXML private Button pauseBtn;
  @FXML private Pane canvasArea;
  @FXML private Canvas canvas;
  @FXML private Canvas overlay;

  private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon(true).factory());

  private MazeGenerator generator;
  private MazeTuner tuner;
  private MazePainter painter;
  private GridGraph topology;
  private GridGraph2D generatedMaze;
  private GridGraph2D tunedMaze;
  private int[] weights;
  private CompletableFuture<Void> pause = CompletableFuture.completedFuture(null);
  private boolean canceled;
  private Consumer<Integer> vertexSelector;
  private int source;
  private int destination;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    canvasArea.heightProperty().addListener(this::onResize);
    canvasArea.widthProperty().addListener(this::onResize);
    painter = new MazePainter(null, canvas.getGraphicsContext2D())
        .setWallColor(StaticConfig.wallColor());
    tuner = new MazeTuner()
        .setReliefMinWeight(StaticConfig.minWeight())
        .setReliefMaxSummitWeight(StaticConfig.maxWeight());
    onSrcTool();
  }

  @FXML
  private void onGenerate() {
    if (generator == null) return;

    generationGroup.setDisable(true);
    tuneGroup.setDisable(true);
    tuneGroupAll.setDisable(true);
    solveGroup.setDisable(true);
    overlay.setVisible(false);
    playPauseGroup.setDisable(false);
    pause = CompletableFuture.completedFuture(null);
    canceled = false;

    int side = (int) gridSizeSlider.getValue();
    topology = new GridGraph(side);
    GridGraph.bindAll(topology);
    GridGraph delegate = new GridGraph(side);
    if (!generator.requireWalls())
      GridGraph.bindAll(delegate);
    generatedMaze = delegate;
    var maze = new ObservableMaze(topology, delegate);
    tunedMaze = null;
    weights = new int[topology.nbVertices()];
    Arrays.fill(weights, StaticConfig.minWeight());

    painter.setMaze(maze)
        .setCellColorF(v -> StaticConfig.generatorColor(maze, v));

    repaintMaze();
    repaintOverlay();

    MazeAnimation animation = newAnimation();
    if (animateGen.isSelected())
      maze.subscribe(animation);

    worker.submit(() -> {
      try {
        MazeGenerator builder = generator;
        builder.generate(maze, StaticConfig.startPoint(maze));
      } catch (CanceledAnimationException ignored) {
      } finally {
        maze.unsubscribe(animation);
        source = 0;
        destination = maze.nbVertices() - 1;

        Platform.runLater(() -> {
          // Désactivation/activation des éléments de l'UI commence à devenir confuse, pattern état ?
          generationGroup.setDisable(false);
          tuneGroupAll.setDisable(false);
          playPauseGroup.setDisable(true);

          if (!canceled && autoTune.isSelected()) {
            removeWalls();
            generateRelief();
            painter.setMaze(tunedMaze)
                .setCellColorF(v -> StaticConfig.tuningColor(weights, v));
          }

          if (!animateGen.isSelected())
            repaintMaze();

          if (!canceled) {
            tuneGroup.setDisable(false);
            solveGroup.setDisable(false);
            overlay.setDisable(false);
            overlay.setVisible(true);
            repaintOverlay();
          }
        });
      }
    });
  }

  @FXML
  private void onRemoveWalls() {
    beforeTuning();
    worker.submit(() -> {
      removeWalls();
      Platform.runLater(this::afterTuning);
    });
  }

  // Evite des interactions inutiles avec l'UI (potentiellement très coûteuses)
  private void removeWalls() {
    tuner.setWallRemovalProbability(wallRemovalSlider.getValue() / 1000.0);
    tunedMaze = generatedMaze.copy();
    tuner.removeWalls(topology, tunedMaze);
  }

  @FXML
  private void onGenerateRelief() {
    beforeTuning();
    worker.submit(() -> {
      generateRelief();
      Platform.runLater(this::afterTuning);
    });

  }

  // Même remarque que pour removeWalls
  private void generateRelief() {
    tuner.setReliefDensityFactor(reliefDensitySlider.getValue() / 100);
    tuner.setReliefRadiusRatio((reliefRadiusSlider.maxProperty().get()
        + reliefRadiusSlider.minProperty().get()
        - reliefRadiusSlider.getValue()) / 10);
    weights = tuner.generateRelief(topology.width(), topology.height());
  }

  private void beforeTuning() {
    generationGroup.setDisable(true);
    tuneGroup.setDisable(true);
    solveGroup.setDisable(true);
    overlay.setDisable(true);
  }

  private void afterTuning() {
    if (tunedMaze == null)
      tunedMaze = generatedMaze;

    painter = new MazePainter(tunedMaze, canvas.getGraphicsContext2D())
        .setWallColor(StaticConfig.wallColor())
        .setCellColorF(v -> StaticConfig.tuningColor(weights, v));

    generationGroup.setDisable(false);
    tuneGroup.setDisable(false);
    solveGroup.setDisable(false);
    overlay.setDisable(false);
    overlay.setVisible(true);

    repaintMaze();
    repaintOverlay();
  }

  @FXML
  private void onSolve() {
    generationGroup.setDisable(true);
    tuneGroupAll.setDisable(true);
    solveGroup.setDisable(true);
    overlay.setDisable(true);
    playPauseGroup.setDisable(false);
    pause = CompletableFuture.completedFuture(null);
    canceled = false;

    GridMazeSolver solver = solverChoiceBox.getValue().value();
    if (solver == null) return;

    ObservableMaze maze = new ObservableMaze(topology, tunedMaze == null ? generatedMaze : tunedMaze);
    MazeAnimation animation = newAnimation();
    maze.subscribe(animation);

    SolverMonitor resolutionMonitor = new SolverMonitor(maze.nbVertices(), animation);
    // Utilisé pour l'affichage, les données sont déjà connues
    int[] pathPred = new int[maze.nbVertices()];
    Arrays.fill(pathPred, -1);
    var wf = new ShenaniganWeightFunction(weights, StaticConfig.minWeight());
    painter
        .setWallColor(StaticConfig.wallColor())
        .setCellColorF(v -> StaticConfig.solverCellColor(weights, resolutionMonitor, pathPred, wf, v));

    // Efface la solution précédente
    repaintMaze();

    worker.submit(() -> {
      try {
        var r = solver.solve(maze, wf, source, destination, resolutionMonitor);

        int pred = source;
        for (int v : r.path()) {
          pathPred[v] = pred;
          animation.onVertexChanged(v);
          pred = v;
        }

        Platform.runLater(() -> {
            resultLength.setText(String.valueOf(r.length()));
            resultPerformance.setText(String.valueOf(r.treatments()));
        });
      } catch (CanceledAnimationException ignored) {
      } finally {
        maze.unsubscribe(animation);
        Platform.runLater(() -> {
          generationGroup.setDisable(false);
          tuneGroupAll.setDisable(false);
          playPauseGroup.setDisable(true);
          solveGroup.setDisable(false);
          overlay.setDisable(false);
        });
      }
    });
  }

  @FXML
  private void onSrcTool() {
    vertexSelector = v -> source = v;
  }
  @FXML
  private void onDstTool() {
    vertexSelector = v -> destination = v;
  }

  @FXML
  private void onSelectVertex(MouseEvent event) {
    // Très approximatif dans les bords droit et inférieur
    int l = painter.getCellSide() + 2 * painter.getWallThickness();
    int row = (int) event.getY() / l;
    int col = (int) event.getX() / l;

    // D'où cette vérification
    if (row < 0 || col < 0 || row >= generatedMaze.width() || col >= generatedMaze.height()) return;

    vertexSelector.accept(row * generatedMaze.width() + col);
    repaintOverlay();
  }

  @FXML
  private void onPlayPause() {
    setPause(pause.isDone());
  }

  @FXML
  private void onStop() {
    setPause(false);
    canceled = true;
    pause = CompletableFuture.failedFuture(new CanceledAnimationException());
  }

  private void setPause(boolean paused) {
    if (paused) {
      pause = new CompletableFuture<>();
      // Chars ⏵/⏸/⏹ ne fonctionnent pas avec les DE basés sur GTK, utilisation de texte
      pauseBtn.textProperty().setValue("Play");
    } else {
      pause.complete(null);
      pauseBtn.textProperty().setValue("Pause");
    }
  }

  @FXML
  private void onOpen() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open maze");
    File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());

    if (file == null) return;

    var r = MazeIO.read(file);
    var maze = r.maze();

    onStop();
    topology = new GridGraph(maze.width(), maze.height());
    GridGraph.bindAll(topology);

    generatedMaze = maze; // Perte du labyrinthe initial (non enregistré dans le fichier). Pas grave.
    tunedMaze = maze;
    this.weights = r.weights();
    painter.setCellColorF(v -> StaticConfig.tuningColor(weights, v));

    tuneGroup.setDisable(false);
    solveGroup.setDisable(false);

    source = 0;
    destination = maze.nbVertices() - 1;
    repaintMaze();
    repaintOverlay();
  }

  @FXML
  private void onSave() {
    var maze = tunedMaze == null ? generatedMaze : tunedMaze;

    if (maze == null) return;

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save maze");
    File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());

    if (file == null) return;

    MazeIO.write(maze, weights, file);
  }

  private void onResize(Observable ignored) {
    if (generatedMaze == null) return;

    repaintMaze();
    repaintOverlay();
  }

  private void repaintOverlay() {
    overlay.setWidth(canvas.getWidth());
    overlay.setHeight(canvas.getHeight());

    GraphicsContext context = overlay.getGraphicsContext2D();
    context.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

    paintSelection(source, Color.GREEN);
    paintSelection(destination, Color.RED);
  }

  private void paintSelection(int vertex, Color color) {
    int x = painter.cellOffset(vertex % generatedMaze.width());
    int y = painter.cellOffset(vertex / generatedMaze.height());

    GraphicsContext context = overlay.getGraphicsContext2D();

    context.setFill(color);
    context.fillOval(x, y, painter.getCellSide(), painter.getCellSide());
  }

  private void repaintMaze() {
    GridGraph2D maze = tunedMaze == null ? generatedMaze : tunedMaze;
    painter.setMaze(maze);
    int side = (int) Math.min(canvasArea.getWidth(), canvasArea.getHeight());

    // Permet de déterminer une taille de mur pertinente sans connaître à l'avance la taille réelle de la cellule
    int approximateCellSide = side / maze.width();
    int wallThickness = StaticConfig.wallThickness(approximateCellSide);
    int doubleThickness = 2 * wallThickness;

    // Garanti une taille min de 1 pour éviter une image noire
    int cellSide = Math.max(1, (side - doubleThickness) / maze.width() - doubleThickness);
    side = (cellSide + doubleThickness) * maze.width() + doubleThickness;

    canvas.setWidth(side);
    canvas.setHeight(side);

    painter.setWallThickness(wallThickness)
          .setCellSide(cellSide);
    painter.repaint();
  }

  public void init(MazeGenerator generator, List<ChoiceItem<GridMazeSolver>> solvers) {
    this.generator = generator;
    solverChoiceBox.setItems(FXCollections.observableList(solvers));
    solverChoiceBox.setValue(solvers.getFirst());
  }

  private MazeAnimation newAnimation() {
    return new MazeAnimation(painter,
            () -> pause,
            // Fonction à décroissance géométrique fournissant un résultat dans [0.01;slider.max-0.99]
            // Permet de lisser l'accélération de l'animation lors de la sélection de faibles valeurs
            () -> Math.pow(
                    delaySlider.getMax(),
                    (delaySlider.getMax() - delaySlider.getValue()) / delaySlider.getMax()) - 0.99);
  }

}