package ch.heig.gre.gui.impl;

import ch.heig.gre.graph.GraphObserver;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class MazeAnimation implements GraphObserver {
  private final MazePainter painter;
  private final Supplier<CompletableFuture<Void>> pauseControl;
  private final DoubleSupplier dynamicDelay; // [ms]

  public MazeAnimation(MazePainter painter, Supplier<CompletableFuture<Void>> pauseControl, DoubleSupplier dynamicDelay) {
    this.painter = painter;
    this.pauseControl = pauseControl;
    this.dynamicDelay = dynamicDelay;
  }

  @Override
  public void onEdgeAdded(int u, int v) {
    Platform.runLater(() -> painter.drawWall(u, v));
     pause();
  }

  @Override
  public void onEdgeRemoved(int u, int v) {
    onEdgeAdded(u, v);
  }

  @Override
  public void onVertexChanged(int v) {
    Platform.runLater(() -> painter.drawCell(v));
    pause();
  }

  private void pause() {
    try {
      pauseControl.get().join();
      int ms = (int) dynamicDelay.getAsDouble();
      int ns = (int) ((dynamicDelay.getAsDouble() - ms) * 1_000_000);
      Thread.sleep(ms, ns);
    } catch (InterruptedException | CompletionException e) {
      throw new CanceledAnimationException();
    }
  }
}
