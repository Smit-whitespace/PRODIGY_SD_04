import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SudokuSolverGUI
 * Single-file Swing app that lets you type a Sudoku puzzle and solve it using backtracking.
 *
 * Usage:
 *   javac SudokuSolverGUI.java
 *   java SudokuSolverGUI
 */
public class SudokuSolverGUI extends JFrame {
    private final JTextField[][] cells = new JTextField[9][9];
    private final Font cellFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    public SudokuSolverGUI() {
        super("Sudoku Solver — enter puzzle and press Solve");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel board = buildBoardPanel();
        add(board, BorderLayout.CENTER);

        JPanel control = buildControlPanel();
        add(control, BorderLayout.SOUTH);

        setSize(600, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildBoardPanel() {
        JPanel board = new JPanel(new GridLayout(9, 9));
        board.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                JTextField tf = new JTextField();
                tf.setHorizontalAlignment(JTextField.CENTER);
                tf.setFont(cellFont);
                // Limit input to single digit 1-9
                ((AbstractDocument) tf.getDocument()).setDocumentFilter(new DigitFilter());
                // Visuals for 3x3 blocks
                int top = (r % 3 == 0) ? 4 : 1;
                int left = (c % 3 == 0) ? 4 : 1;
                int bottom = (r == 8) ? 4 : 1;
                int right = (c == 8) ? 4 : 1;
                tf.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.DARK_GRAY));
                cells[r][c] = tf;
                board.add(tf);
            }
        }
        return board;
    }

    private JPanel buildControlPanel() {
        JPanel control = new JPanel();
        control.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        JButton solveBtn = new JButton("Solve");
        JButton clearBtn = new JButton("Clear");
        JButton exampleBtn = new JButton("Load Example");

        solveBtn.addActionListener(e -> onSolve());
        clearBtn.addActionListener(e -> onClear());
        exampleBtn.addActionListener(e -> loadExample());

        control.add(solveBtn);
        control.add(clearBtn);
        control.add(exampleBtn);

        return control;
    }

    // When Solve clicked
    private void onSolve() {
        int[][] grid = new int[9][9];

        // Read and validate inputs
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = cells[r][c].getText().trim();
                if (text.isEmpty()) {
                    grid[r][c] = 0;
                } else {
                    try {
                        int v = Integer.parseInt(text);
                        if (v < 1 || v > 9) throw new NumberFormatException();
                        grid[r][c] = v;
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid entry at row " + (r+1) + ", col " + (c+1) + ". Only digits 1-9 allowed or empty.",
                                "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
        }

        // Validate puzzle (no immediate conflicts)
        if (!isInitialGridValid(grid)) {
            JOptionPane.showMessageDialog(this,
                    "The initial grid has conflicts (duplicate numbers in a row, column, or 3x3 block). Fix them first.",
                    "Invalid Puzzle", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Solve (use SwingWorker to avoid UI freeze for large puzzles)
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return solveSudoku(grid);
            }

            @Override
            protected void done() {
                try {
                    boolean solved = get();
                    if (solved) {
                        // Copy back to UI
                        for (int r = 0; r < 9; r++) {
                            for (int c = 0; c < 9; c++) {
                                cells[r][c].setText(Integer.toString(grid[r][c]));
                            }
                        }
                        JOptionPane.showMessageDialog(SudokuSolverGUI.this, "Solved! ✅", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SudokuSolverGUI.this, "No solution exists for the given puzzle.", "Unsolvable", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SudokuSolverGUI.this, "An error occurred while solving.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Clear board
    private void onClear() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                cells[r][c].setText("");
    }

    // Example puzzle (moderate difficulty)
    private void loadExample() {
        int[][] example = {
            {0,0,0, 2,6,0, 7,0,1},
            {6,8,0, 0,7,0, 0,9,0},
            {1,9,0, 0,0,4, 5,0,0},

            {8,2,0, 1,0,0, 0,4,0},
            {0,0,4, 6,0,2, 9,0,0},
            {0,5,0, 0,0,3, 0,2,8},

            {0,0,9, 3,0,0, 0,7,4},
            {0,4,0, 0,5,0, 0,3,6},
            {7,0,3, 0,1,8, 0,0,0}
        };
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                cells[r][c].setText(example[r][c] == 0 ? "" : Integer.toString(example[r][c]));
    }

    // Validate initial grid: ensure no duplicates in any row, col or block
    private boolean isInitialGridValid(int[][] grid) {
        for (int r = 0; r < 9; r++) {
            boolean[] seen = new boolean[10];
            for (int c = 0; c < 9; c++) {
                int v = grid[r][c];
                if (v != 0) {
                    if (seen[v]) return false;
                    seen[v] = true;
                }
            }
        }
        for (int c = 0; c < 9; c++) {
            boolean[] seen = new boolean[10];
            for (int r = 0; r < 9; r++) {
                int v = grid[r][c];
                if (v != 0) {
                    if (seen[v]) return false;
                    seen[v] = true;
                }
            }
        }
        for (int br = 0; br < 3; br++) {
            for (int bc = 0; bc < 3; bc++) {
                boolean[] seen = new boolean[10];
                for (int r = br*3; r < br*3+3; r++) {
                    for (int c = bc*3; c < bc*3+3; c++) {
                        int v = grid[r][c];
                        if (v != 0) {
                            if (seen[v]) return false;
                            seen[v] = true;
                        }
                    }
                }
            }
        }
        return true;
    }

    // Backtracking solver
    private boolean solveSudoku(int[][] grid) {
        int[] pos = findEmpty(grid);
        if (pos == null) return true; // solved
        int r = pos[0], c = pos[1];

        for (int num = 1; num <= 9; num++) {
            if (isSafe(grid, r, c, num)) {
                grid[r][c] = num;
                if (solveSudoku(grid)) return true;
                grid[r][c] = 0; // backtrack
            }
        }
        return false;
    }

    // Find empty cell, return {r,c} or null if none
    private int[] findEmpty(int[][] grid) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (grid[r][c] == 0)
                    return new int[]{r, c};
        return null;
    }

    // Check if placing num at r,c is valid
    private boolean isSafe(int[][] g, int r, int c, int num) {
        // row
        for (int col = 0; col < 9; col++)
            if (g[r][col] == num) return false;
        // col
        for (int row = 0; row < 9; row++)
            if (g[row][c] == num) return false;
        // block
        int br = (r / 3) * 3, bc = (c / 3) * 3;
        for (int row = br; row < br + 3; row++)
            for (int col = bc; col < bc + 3; col++)
                if (g[row][col] == num) return false;
        return true;
    }

    // DocumentFilter to allow only 1 char digits 1-9
    static class DigitFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (isAllowed(fb.getDocument(), string)) {
                super.insertString(fb, offset, string, attr);
            } // else ignore
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (isAllowed(fb.getDocument(), text)) {
                // replace entire content to ensure single char
                String newText = text == null ? "" : text;
                fb.replace(0, fb.getDocument().getLength(), newText, attrs);
            }
        }
        private boolean isAllowed(Document doc, String text) {
            try {
                String existing = doc.getText(0, doc.getLength());
                String candidate = existing + (text == null ? "" : text);
                // Normalize candidate (we'll allow replacing so ensure final length <=1)
                if (candidate.length() > 1) return false;
                if (candidate.isEmpty()) return true;
                char ch = candidate.charAt(0);
                return ch >= '1' && ch <= '9';
            } catch (BadLocationException e) {
                return false;
            }
        }
    }

    public static void main(String[] args) {
        // Set the look and feel to system default for nicer appearance
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(SudokuSolverGUI::new);
    }
}
