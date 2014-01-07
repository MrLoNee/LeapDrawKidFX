/**
 * RadialMenuEnhancedDemo
 * Copyright 2013 (C) Mr LoNee - (Laurent NICOLAS) - www.mrlonee.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.mrlonee.leap.drawkidfx;

import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.animation.FadeTransitionBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.mrlonee.leap.drawkidfx.DrawFxLeapListener.Interraction;

public class DrawKidFx extends Application {

    public static void main(final String[] args) {
	launch(args);
    }

    private Group drawContainer;
    private RadialColorMenu radialMenu;
    private Polyline currentDrawingLine;
    private Group container;

    @Override
    public void start(final Stage primaryStage) throws Exception {
	container = new Group();
	final Scene scene = new Scene(container, Color.WHITE);
	primaryStage.initStyle(StageStyle.UNDECORATED);
	primaryStage.setResizable(false);
	primaryStage.setScene(scene);
	primaryStage.centerOnScreen();
	final Dimension screenSize = Toolkit.getDefaultToolkit()
		.getScreenSize();
	primaryStage.setWidth(screenSize.getWidth());
	primaryStage.setHeight(screenSize.getHeight());
	primaryStage.toFront();

	final String cursorPath = "images/icons/gemicon/PNG/32x32/row 13/3.png";
	final Cursor cursor = new ImageCursor(new Image(new FileInputStream(
		cursorPath)));
	scene.setCursor(cursor);

	drawContainer = new Group();

	radialMenu = new RadialColorMenu();

	radialMenu.translateXProperty().bind(scene.widthProperty().divide(2.0));
	radialMenu.translateYProperty()
		.bind(scene.heightProperty().divide(2.0));
	radialMenu.setVisible(false);
	radialMenu.setOpacity(0.0);
	container.getChildren().addAll(drawContainer, radialMenu);

	final DrawFxLeapListener leapListener = new DrawFxLeapListener();
	final Robot robot = new Robot();

	leapListener.interractionProperty().addListener(
		new ChangeListener<Interraction>() {

		    @Override
		    public void changed(
			    final ObservableValue<? extends Interraction> obsValue,
			    final Interraction previous,
			    final Interraction current) {
			Platform.runLater(new Runnable() {
			    @Override
			    public void run() {
				if (current == Interraction.HAND_OPENED) {
				    if (previous != Interraction.HAND_OPENED
					    && previous != Interraction.SWIPPED) {
					changeCurrentDrawing();
					showColorMenu(true);
				    }
				} else if (current == Interraction.FINGER_POINTED) {
				    if (previous == Interraction.HAND_OPENED) {
					// Change the painting color with the
					// currently selected color
					showColorMenu(false);
				    }
				} else if (current == Interraction.SWIPPED) {
				    if (current == Interraction.HAND_OPENED) {
					showColorMenu(false);
				    }
				    drawContainer.getChildren().clear();
				}
			    }

			});
		    }
		});

	leapListener.handPositionProperty().addListener(
		new ChangeListener<Point3D>() {
		    @Override
		    public void changed(
			    final ObservableValue<? extends Point3D> obsValue,
			    final Point3D prev, final Point3D current) {
			robot.mouseMove((int) current.getX(),
				(int) current.getY());
		    }
		});

	leapListener.fingerPositionProperty().addListener(
		new ChangeListener<Point3D>() {

		    @Override
		    public void changed(
			    final ObservableValue<? extends Point3D> obsValue,
			    final Point3D prev, final Point3D current) {
			Platform.runLater(new Runnable() {
			    @Override
			    public void run() {
				robot.mouseMove((int) current.getX(),
					(int) current.getY());
			    }

			});

//			Platform.runLater(new Runnable() {
//			    @Override
//			    public void run() {
//
//				if (current.getZ() < 0) {
//				    if (prev == null || prev.getZ() > 0) {
//					changeCurrentDrawing();
//				    }
//
//				    currentDrawingLine.getPoints().add(
//					    current.getX());
//				    currentDrawingLine.getPoints().add(
//					    current.getY());
//				}
//			    }
//
//			});
		    }
		});

	final Controller leapController = new Controller();
	leapController.enableGesture(Gesture.Type.TYPE_SWIPE);

	final ScheduledExecutorService leapPollingThread = Executors
		.newSingleThreadScheduledExecutor();
	leapPollingThread.scheduleWithFixedDelay(new Runnable() {

	    @Override
	    public void run() {
		leapListener.onFrame(leapController);
	    }

	}, 10, (int) (1000d / 60d), TimeUnit.MILLISECONDS);
	primaryStage.show();

    }

    private void changeCurrentDrawing() {
	if (currentDrawingLine != null) {
	    currentDrawingLine.strokeProperty().unbind();
	}
	currentDrawingLine = new Polyline();
	currentDrawingLine.strokeProperty().bind(
		radialMenu.selectedColorProperty());
	currentDrawingLine.setStrokeWidth(3);
	drawContainer.getChildren().add(currentDrawingLine);

    }

    private void showColorMenu(final boolean visible) {
	if (visible) {
	    radialMenu.setVisible(true);
	    FadeTransitionBuilder.create().node(radialMenu)
		    .duration(Duration.millis(200)).fromValue(0.0).toValue(1.0)
		    .build().play();
	} else {
	    FadeTransitionBuilder.create().node(radialMenu)
		    .duration(Duration.millis(200)).fromValue(1.0).toValue(0.0)
		    .onFinished(new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent event) {
			    radialMenu.setVisible(false);
			}

		    }).build().play();
	}
    }

    int snapshotCounter = 0;

    private void takeSnapshot(final Scene scene) {
	// Take snapshot of the scene
	final WritableImage writableImage = scene.snapshot(null);

	// Write snapshot to file system as a .png image
	final File outFile = new File("snapshot/radialmenugame-snapshot-"
		+ snapshotCounter + ".png");
	outFile.getParentFile().mkdirs();
	try {
	    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png",
		    outFile);
	} catch (final IOException ex) {
	    System.out.println(ex.getMessage());
	}

	snapshotCounter++;
    }

    ImageView getImageView(final String path) {
	ImageView imageView = null;
	try {
	    imageView = ImageViewBuilder.create()
		    .image(new Image(new FileInputStream(path))).build();
	} catch (final FileNotFoundException e) {
	    e.printStackTrace();
	}
	assert (imageView != null);
	return imageView;

    }

}
