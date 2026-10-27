import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.*;

public class CalculatorWithUndo extends JFrame {

    private final JTextField display = new JTextField("0");
    private final JTextArea historyArea = new JTextArea();
    private final JLabel stackInfo = new JLabel("Undo stack: 0 states");
    private final Deque<State> undoStack = new ArrayDeque<>();
    private final Deque<State> redoStack = new ArrayDeque<>();

    private double storedValue = 0;
    private String pendingOperator = "";
    private boolean startNewNumber = true;

    private static class State {
        String displayText;
        String historyText;
        double storedValue;
        String pendingOperator;
        boolean startNewNumber;

        State(CalculatorWithUndo c) {
            displayText = c.display.getText();
            historyText = c.historyArea.getText();
            storedValue = c.storedValue;
            pendingOperator = c.pendingOperator;
            startNewNumber = c.startNewNumber;
        }

        void restore(CalculatorWithUndo c) {
            c.display.setText(displayText);
            c.historyArea.setText(historyText);
            c.storedValue = storedValue;
            c.pendingOperator = pendingOperator;
            c.startNewNumber = startNewNumber;
        }
    }

    public CalculatorWithUndo() {
        setTitle("Stack Calculator | Java Project");
        setSize(420, 690);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel main = new JPanel(new BorderLayout(10, 12));
        main.setBackground(new Color(37, 76, 126));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(main);

        JLabel title = new JLabel("STACK CALCULATOR", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        main.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 12));
        center.setOpaque(false);
        main.add(center, BorderLayout.CENTER);

        display.setFont(new Font("Arial", Font.BOLD, 34));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setForeground(Color.WHITE);
        display.setBackground(new Color(100, 143, 190));
        display.setCaretColor(Color.WHITE);
        display.setEditable(false);
        display.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        center.add(display, BorderLayout.NORTH);

        String[] labels = {
            "Undo", "Redo", "C", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "0", ".", "%", "="
        };

        JPanel buttons = new JPanel(new GridLayout(5, 4, 8, 8));
        buttons.setOpaque(false);

        for (String label : labels) {
            JButton button = new JButton(label);
            button.setFont(new Font("Arial", Font.BOLD, 22));
            button.setForeground(Color.BLACK);
            button.setBackground(Color.WHITE);
            button.setOpaque(true);
            button.setBorderPainted(true);
            button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(85, 65));

            if (label.equals("Undo")) {
                button.setBackground(new Color(170, 225, 190));
            } else if (label.equals("Redo")) {
                button.setBackground(new Color(255, 205, 130));
            } else if (label.equals("C")) {
                button.setBackground(new Color(255, 160, 160));
            } else if ("÷×-+=".contains(label)) {
                button.setBackground(new Color(190, 215, 245));
            }

            button.addActionListener(e -> handleButton(label));
            buttons.add(button);
        }

        center.add(buttons, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);

        JLabel historyTitle = new JLabel("Calculation History");
        historyTitle.setForeground(Color.WHITE);
        historyTitle.setFont(new Font("Arial", Font.BOLD, 14));

        historyArea.setEditable(false);
        historyArea.setFont(new Font("Arial", Font.PLAIN, 13));
        historyArea.setForeground(Color.BLACK);
        historyArea.setBackground(new Color(235, 242, 250));
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(historyArea);
        scroll.setPreferredSize(new Dimension(370, 65));

        stackInfo.setForeground(new Color(190, 255, 200));
        stackInfo.setFont(new Font("Arial", Font.BOLD, 13));

        JLabel lifo = new JLabel("LIFO: Last In, First Out");
        lifo.setForeground(Color.WHITE);
        lifo.setFont(new Font("Arial", Font.ITALIC, 13));

        bottom.add(historyTitle);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(scroll);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(stackInfo);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(lifo);

        main.add(bottom, BorderLayout.SOUTH);
        updateStackInfo();
    }

    private void handleButton(String key) {
        if (key.equals("Undo")) {
            undo();
            return;
        }

        if (key.equals("Redo")) {
            redo();
            return;
        }

        // Save the current state before a new action.
        undoStack.push(new State(this));
        redoStack.clear();

        try {
            if (key.matches("[0-9]")) {
                enterDigit(key);
            } else if (key.equals(".")) {
                enterDecimal();
            } else if ("+-×÷".contains(key)) {
                chooseOperator(key);
            } else if (key.equals("=")) {
                calculate();
            } else if (key.equals("C")) {
                clearCalculator();
            } else if (key.equals("%")) {
                double value = getDisplayValue() / 100;
                display.setText(format(value));
                startNewNumber = true;
            }
        } catch (ArithmeticException ex) {
            display.setText("Cannot divide by zero");
            storedValue = 0;
            pendingOperator = "";
            startNewNumber = true;
        }

        updateStackInfo();
    }

    private void enterDigit(String digit) {
        if (startNewNumber || display.getText().equals("0")) {
            display.setText(digit);
            startNewNumber = false;
        } else {
            display.setText(display.getText() + digit);
        }
    }

    private void enterDecimal() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    private void chooseOperator(String op) {
        double current = getDisplayValue();

        if (!pendingOperator.isEmpty() && !startNewNumber) {
            storedValue = applyOperation(
                storedValue, current, pendingOperator
            );
            display.setText(format(storedValue));
        } else {
            storedValue = current;
        }

        pendingOperator = op;
        startNewNumber = true;
    }

    private void calculate() {
        if (pendingOperator.isEmpty()) {
            return;
        }

        double secondValue = getDisplayValue();
        double firstValue = storedValue;
        String op = pendingOperator;

        double result = applyOperation(firstValue, secondValue, op);

        historyArea.append(
            format(firstValue) + " " + op + " "
            + format(secondValue) + " = "
            + format(result) + "\n"
        );

        display.setText(format(result));
        storedValue = result;
        pendingOperator = "";
        startNewNumber = true;
    }

    private double applyOperation(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "×": return a * b;
            case "÷":
                if (b == 0) {
                    throw new ArithmeticException();
                }
                return a / b;
            default: return b;
        }
    }

    private void clearCalculator() {
        display.setText("0");
        storedValue = 0;
        pendingOperator = "";
        startNewNumber = true;
        historyArea.setText("");
    }

    private double getDisplayValue() {
        return Double.parseDouble(display.getText());
    }

    private String format(double value) {
        if (!Double.isFinite(value)) {
            return "Error";
        }
        return BigDecimal.valueOf(value)
            .stripTrailingZeros().toPlainString();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }

        redoStack.push(new State(this));
        undoStack.pop().restore(this);
        updateStackInfo();
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }

        undoStack.push(new State(this));
        redoStack.pop().restore(this);
        updateStackInfo();
    }

    private void updateStackInfo() {
        stackInfo.setText(
            "Undo stack: " + undoStack.size()
            + " states | Redo stack: " + redoStack.size()
            + " states"
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CalculatorWithUndo calculator = new CalculatorWithUndo();
            calculator.setVisible(true);
        });
    }
}