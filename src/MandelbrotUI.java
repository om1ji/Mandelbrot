import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionListener;

public class MandelbrotUI extends JPanel {
    /**
     * Класс для создания интерфейса пользователя для приложения Мандельброта.
     */
    private JSlider colorSlider;
    private JSlider iterSlider;
    private JButton resetButton;
    private JButton undoButton;

    /**
     * Конструктор для создания экземпляра MandelbrotUI.
     */
    public MandelbrotUI() {
        setLayout(new GridLayout(2, 2));

        colorSlider = new JSlider(0, 100, 0);
        iterSlider = new JSlider(100, 2000, 1000);
        resetButton = new JButton("Reset View");
        undoButton = new JButton("Undo");

        add(new JLabel("Color Offset:"));
        add(colorSlider);
        add(new JLabel("Max Iterations:"));
        add(iterSlider);
        add(resetButton);
        add(undoButton);
    }

    /**
     * Добавляет слушатель для слайдера цвета.
     * @param listener слушатель для добавления.
     */
    public void addColorSliderListener(ChangeListener listener) {
        colorSlider.addChangeListener(listener);
    }

    /**
     * Добавляет слушатель для слайдера итераций.
     * @param listener слушатель для добавления.
     */
    public void addIterSliderListener(ChangeListener listener) {
        iterSlider.addChangeListener(listener);
    }

    /**
     * Добавляет слушатель для кнопки сброса.
     * @param listener слушатель для добавления.
     */
    public void addResetButtonListener(ActionListener listener) {
        resetButton.addActionListener(listener);
    }

    /**
     * Добавляет слушатель для кнопки отмены.
     * @param listener слушатель для добавления.
     */
    public void addUndoButtonListener(ActionListener listener) {
        undoButton.addActionListener(listener);
    }

    /**
     * Возвращает значение смещения цвета.
     * @return значение смещения цвета.
     */
    public int getColorOffset() {
        return colorSlider.getValue();
    }

    /**
     * Возвращает значение максимального количества итераций.
     * @return значение максимального количества итераций.
     */
    public int getMaxIter() {
        return iterSlider.getValue();
    }
}