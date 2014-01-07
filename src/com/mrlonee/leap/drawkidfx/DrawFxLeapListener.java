package com.mrlonee.leap.drawkidfx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Screen;
import com.leapmotion.leap.Vector;

public class DrawFxLeapListener extends Listener {

    public enum Interraction {
	HAND_OPENED, FINGER_POINTED, SWIPPED, NONE
    }

    private ObjectProperty<Point3D> fingerPositionProperty = new SimpleObjectProperty<Point3D>();
    private ObjectProperty<Point3D> handPositionProperty = new SimpleObjectProperty<Point3D>();
    private ObjectProperty<Interraction> interractionProperty = new SimpleObjectProperty<Interraction>(
	    Interraction.NONE);

    private long lastSwipeTime;

    public ObjectProperty<Interraction> interractionProperty() {
	return interractionProperty;
    }

    public ObjectProperty<Point3D> handPositionProperty() {
	return handPositionProperty;
    }

    public ObservableValue<Point3D> fingerPositionProperty() {
	return fingerPositionProperty;
    }

    @Override
    public void onConnect(final Controller controller) {
	System.out.println("Leap Connected");
	controller.enableGesture(Gesture.Type.TYPE_SWIPE);
    }

    @Override
    public void onFrame(final Controller controller) {
	final Frame frame = controller.frame();
	final Screen screen = controller.locatedScreens().get(0);
	final GestureList gestures = frame.gestures();
	boolean swipped = false;
	for (int i = 0; i < gestures.count(); i++) {
	    final Gesture gesture = gestures.get(i);
	    if (gesture.type() == Gesture.Type.TYPE_SWIPE) {
		if (gesture.state() == Gesture.State.STATE_START) {
		    final long swipeTime = System.currentTimeMillis();
		    if (swipeTime - lastSwipeTime > 2000) {
			lastSwipeTime = swipeTime;
			interractionProperty.set(Interraction.SWIPPED);
			swipped = true;
		    }
		}
	    }
	}
	if(!swipped){
		if (frame.fingers().count() == 1) {
		    final Finger finger = frame.fingers().get(0);
		    if (finger.isValid()) {
			interractionProperty.set(Interraction.FINGER_POINTED);
			final Vector intersect = screen.intersect(
				finger.stabilizedTipPosition(), finger.direction(),
				true);
			final double z = finger.stabilizedTipPosition().getZ();
			final Point3D point = new Point3D(screen.widthPixels()
				* Math.min(1d, Math.max(0d, intersect.getX())),
				screen.heightPixels()
					* Math.min(1d,
						Math.max(0d, (1d - intersect.getY()))),
				z);
			fingerPositionProperty.setValue(point);
		    }
		}

		else if (frame.hands().count() == 1 && frame.fingers().count() >= 4) {
		    final Hand hand = frame.hands().get(0);
		    if (hand.isValid()) {
			interractionProperty.set(Interraction.HAND_OPENED);
			final Vector intersect = screen.intersect(
				hand.stabilizedPalmPosition(), hand.direction(), true);
			final double z = hand.stabilizedPalmPosition().getZ();
			final Point3D point = new Point3D(screen.widthPixels()
				* Math.min(1d, Math.max(0d, intersect.getX())),
				screen.heightPixels()
					* Math.min(1d,
						Math.max(0d, (1d - intersect.getY()))),
				z);
			handPositionProperty.setValue(point);
		    }
		}
	}
    }

}