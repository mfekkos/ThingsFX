/*
 * This file is part of ThingsFX.
 *
 * ThingsFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ThingsFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ThingsFX. If not, see <http://www.gnu.org/licenses/>.
 */

package com.thingsfx.widget.swing;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

public class SwingView extends Group {

    private static final Map<EventType<?>, Integer> mouseEventMap;
    static {
        Map<EventType<?>, Integer> map = new HashMap<EventType<?>, Integer>();
        map.put(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_PRESSED);
        map.put(javafx.scene.input.MouseEvent.MOUSE_RELEASED,
                MouseEvent.MOUSE_RELEASED);
        map.put(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
                MouseEvent.MOUSE_ENTERED);
        map.put(javafx.scene.input.MouseEvent.MOUSE_EXITED,
                MouseEvent.MOUSE_EXITED);
        map.put(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                MouseEvent.MOUSE_MOVED);
        map.put(javafx.scene.input.MouseEvent.MOUSE_DRAGGED,
                MouseEvent.MOUSE_DRAGGED);
        mouseEventMap = Collections.unmodifiableMap(map);
    }

    private class MouseEventHandler implements
            EventHandler<javafx.scene.input.MouseEvent> {

        @Override
        public void handle(javafx.scene.input.MouseEvent jfxMouseEvent) {
            EventType<?> type = jfxMouseEvent.getEventType();
            int id = mouseEventMap.get(type);
            int button = getAWTButton(jfxMouseEvent.getButton());
            int modifiers = getAWTModifiers(jfxMouseEvent);
            final MouseEvent awtEvent =
                    new MouseEvent(component, id, System.currentTimeMillis(),
                                   modifiers,
                                   (int) jfxMouseEvent.getX(),
                                   (int) jfxMouseEvent.getY(),
                                   (int) jfxMouseEvent.getScreenX(),
                                   (int) jfxMouseEvent.getScreenY(),
                                   jfxMouseEvent.getClickCount(),
                                   false,
                                   button);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    SwingFXEventDispatcher.dispatchEvent(awtEvent, component);
                }
            });
        }

        private int getAWTModifiers(javafx.scene.input.MouseEvent jfxMouseEvent) {
            int mods = 0;
            if (jfxMouseEvent.isAltDown()) {
                mods |= MouseEvent.ALT_MASK | MouseEvent.ALT_DOWN_MASK;
            }
            if (jfxMouseEvent.isControlDown()) {
                mods |= MouseEvent.CTRL_MASK | MouseEvent.CTRL_DOWN_MASK;
            }
            if (jfxMouseEvent.isShiftDown()) {
                mods |= MouseEvent.SHIFT_MASK | MouseEvent.SHIFT_DOWN_MASK;
            }
            if (jfxMouseEvent.isMetaDown()) {
                mods |= MouseEvent.META_MASK | MouseEvent.META_DOWN_MASK;
            }
            if (jfxMouseEvent.isPrimaryButtonDown()) {
                mods |= MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON1_DOWN_MASK;
            }
            if (jfxMouseEvent.isSecondaryButtonDown()) {
                mods |= MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON2_DOWN_MASK;
            }
            if (jfxMouseEvent.isMiddleButtonDown()) {
                mods |= MouseEvent.BUTTON3_MASK | MouseEvent.BUTTON3_DOWN_MASK;
            }

            return mods;
        }

        private int getAWTButton(MouseButton button) {
            switch (button) {
            case PRIMARY:
                return MouseEvent.BUTTON1;
            case SECONDARY:
                return MouseEvent.BUTTON2;
            case MIDDLE:
                return MouseEvent.BUTTON3;
            case NONE:
            default:
                return MouseEvent.NOBUTTON;
            }
        }
    }

    private JComponent component;

    private ProxyWindow proxy;
    private BufferedImageView imgView;

    public SwingView(JComponent component) {

    	imgView = new BufferedImageView();
    	getChildren().add(imgView);
        RepaintManager repaintManager = RepaintManager
                .currentManager(component);
        if (!(repaintManager instanceof ThingsFXRepaintManager)) {
            throw new IllegalStateException("SwingFX.init() method must be "
                    + "called before any Swing rendering"
                    + "and any JavaFX routine call");
        }

        ((ThingsFXRepaintManager) repaintManager).registerListener(this,
                component);

        this.component = component;

        registerEvents();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                JComponent component = SwingView.this.component;
                proxy = new ProxyWindow(SwingView.this);
                proxy.add(component);
                component.addNotify();

                SwingFXEventDispatcher.setLightweightDispatcher(component);
                component.setVisible(true);
                component.setDoubleBuffered(true);
                component.doLayout();
                component.repaint();
            }
        });
    }

    private void registerEvents() {
        MouseEventHandler handler = new MouseEventHandler();
        setOnMousePressed(handler);
        setOnMouseReleased(handler);
        setOnMouseMoved(handler);
        setOnMouseDragged(handler);
        setOnMouseEntered(handler);
        setOnMouseExited(handler);
        KeyEventHandler keyHandler = new KeyEventHandler(component);
        setOnKeyPressed(keyHandler);
        setOnKeyReleased(keyHandler);
        setOnKeyTyped(keyHandler);
        focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                    Boolean oldValue, Boolean newValue) {
                if (newValue == Boolean.FALSE) {
                    FocusEvent fl = new FocusEvent(component, FocusEvent.FOCUS_LOST);
                    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(fl);
                }
            }
        });
    }

    Component getComponent() {
        return component;
    }

    BufferedImageView getImageView() {
    	return imgView;
    }

}
