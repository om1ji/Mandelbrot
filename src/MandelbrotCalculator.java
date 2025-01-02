import java.awt.Color;

/**
 * Класс для вычисления цветов множества Мандельброта.
 */
public class MandelbrotCalculator {

    private final int maxIter;

    /**
     * Конструктор для создания экземпляра MandelbrotCalculator.
     *
     * @param maxIter максимальное количество итераций для вычисления.
     */
    public MandelbrotCalculator(int maxIter) {
        this.maxIter = maxIter;
    }

    /**
     * Вычисляет цвет для заданной точки в пространстве Мандельброта.
     *
     * @param cx действительная часть комплексного числа.
     * @param cy мнимая часть комплексного числа.
     * @param colorOffset смещение цвета для изменения палитры.
     * @return цвет в формате RGB.
     */
    public int calculateColor(double cx, double cy, float colorOffset) {
        double zx = 0.0, zy = 0.0;
        int iter = 0;
        while (zx * zx + zy * zy < 4.0 && iter < maxIter) {
            double temp = zx * zx - zy * zy + cx;
            zy = 2.0 * zx * zy + cy;
            zx = temp;
            iter++;
        }
        if (iter < maxIter) {
            float hue = 0.6f; // Синий цвет
            float saturation = 1.0f - (iter / (float) maxIter); // Уменьшаем насыщенность к белому
            return Color.HSBtoRGB(hue, saturation, 1.0f);
        } else {
            return Color.BLACK.getRGB();
        }
    }
} 