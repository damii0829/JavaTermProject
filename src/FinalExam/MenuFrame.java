package FinalExam;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class MenuFrame extends JFrame {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "c##dbsoft";

    private final MenuRepository repository = new MenuRepository();
    private final String password;

    private final JTable menuTable = table("메뉴번호", "메뉴명", "메뉴분류");
    private final JTable mealTable = table("식단메뉴번호", "제공일", "식사구분", "메뉴명", "판매가격");
    private final JTable reviewMenuTable = table("식단메뉴번호", "제공일", "식사구분", "메뉴명");
    private final JTable reviewTable = table("리뷰번호", "학번", "별점", "후기", "작성일시");
    private final JTable pollTable = table("회차번호", "회차명", "시작일시", "종료일시", "상태");
    private final JTable candidateTable = table("메뉴번호", "후보메뉴", "득표수");

    private final JTextField mealDate = new JTextField(LocalDate.now().toString(), 10);

    public MenuFrame(String password) {
        super("학생식당 식단·리뷰·메뉴 투표 관리");
        this.password = password;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("메뉴 관리", menuTab());
        tabs.addTab("식단 조회", mealTab());
        tabs.addTab("리뷰", reviewTab());
        tabs.addTab("메뉴 투표", pollTab());
        add(tabs);

        setSize(850, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JPanel menuTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("전체 조회", this::loadMenus));
        actions.add(button("등록", this::insertMenu));
        actions.add(button("수정", this::updateMenu));
        actions.add(button("삭제", this::deleteMenu));
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel mealTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("제공일 (YYYY-MM-DD)"));
        actions.add(mealDate);
        actions.add(button("식단 조회", this::loadMeals));
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(mealTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reviewTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("식단메뉴 불러오기", this::loadReviewMenus));
        actions.add(button("리뷰 작성", this::insertReview));
        actions.add(new JLabel("위 표에서 식단메뉴를 선택하면 아래에 리뷰가 표시됩니다."));

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 8));
        tables.add(new JScrollPane(reviewMenuTable));
        tables.add(new JScrollPane(reviewTable));

        reviewMenuTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadReviews();
            }
        });
        panel.add(actions, BorderLayout.NORTH);
        panel.add(tables, BorderLayout.CENTER);
        return panel;
    }

    private JPanel pollTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("투표회차 불러오기", this::loadPolls));
        actions.add(button("선택한 메뉴에 투표", this::insertVote));
        actions.add(new JLabel("회차를 선택하면 후보가 표시되며 종료 후 득표수를 볼 수 있습니다."));

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 8));
        tables.add(new JScrollPane(pollTable));
        tables.add(new JScrollPane(candidateTable));

        pollTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadCandidates();
            }
        });
        panel.add(actions, BorderLayout.NORTH);
        panel.add(tables, BorderLayout.CENTER);
        return panel;
    }

    private void loadMenus() {
        runDatabase(conn -> fill(menuTable, repository.selectAll(conn)));
    }

    private void insertMenu() {
        JTextField id = new JTextField(15);
        JTextField name = new JTextField(15);
        JTextField category = new JTextField(15);
        Object[] fields = {"메뉴번호", id, "메뉴명", name, "메뉴분류", category};

        if (confirm("메뉴 등록", fields)) {
            runDatabase(conn -> {
                repository.insert(conn, number(id, "메뉴번호"), required(name, "메뉴명"),
                        required(category, "메뉴분류"));
                fill(menuTable, repository.selectAll(conn));
            });
        }
    }

    private void updateMenu() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            message("수정할 메뉴를 선택해 주세요.");
            return;
        }

        Number id = idAt(menuTable, row);
        JTextField name = new JTextField(menuTable.getValueAt(row, 1).toString(), 15);
        JTextField category = new JTextField(menuTable.getValueAt(row, 2).toString(), 15);
        Object[] fields = {"메뉴번호 " + id, "메뉴명", name, "메뉴분류", category};

        if (confirm("메뉴 수정", fields)) {
            runDatabase(conn -> {
                int updated = repository.update(conn, id, required(name, "메뉴명"),
                        required(category, "메뉴분류"));
                if (updated == 0) {
                    message("선택한 메뉴가 이미 삭제됐습니다.");
                }
                fill(menuTable, repository.selectAll(conn));
            });
        }
    }

    private void deleteMenu() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            message("삭제할 메뉴를 선택해 주세요.");
            return;
        }

        Number id = idAt(menuTable, row);
        String name = menuTable.getValueAt(row, 1).toString();
        int choice = JOptionPane.showConfirmDialog(this, id + "번 " + name + " 메뉴를 삭제할까요?",
                "메뉴 삭제", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            runDatabase(conn -> {
                if (repository.delete(conn, id) == 0) {
                    message("선택한 메뉴가 이미 삭제됐습니다.");
                }
                fill(menuTable, repository.selectAll(conn));
            });
        }
    }

    private void loadMeals() {
        LocalDate date;
        try {
            date = LocalDate.parse(mealDate.getText().strip());
        } catch (DateTimeParseException e) {
            message("제공일을 YYYY-MM-DD 형식의 실제 날짜로 입력해 주세요.");
            return;
        }

        if (date.getYear() < 1 || date.getYear() > 9998) {
            message("연도를 0001년부터 9998년 사이로 입력해 주세요.");
            return;
        }

        runDatabase(conn -> fill(mealTable, repository.findMeals(conn, date)));
    }

    private void loadReviewMenus() {
        runDatabase(conn -> {
            fill(reviewMenuTable, repository.findMealMenus(conn));
            fill(reviewTable, List.of());
        });
    }

    private void loadReviews() {
        fill(reviewTable, List.of());
        int row = reviewMenuTable.getSelectedRow();
        if (row >= 0) {
            Number id = idAt(reviewMenuTable, row);
            runDatabase(conn -> fill(reviewTable, repository.findReviews(conn, id)));
        }
    }

    private void insertReview() {
        int row = reviewMenuTable.getSelectedRow();
        if (row < 0) {
            message("리뷰를 작성할 식단메뉴를 선택해 주세요.");
            return;
        }

        Number mealMenuId = idAt(reviewMenuTable, row);
        JTextField id = new JTextField(15);
        JTextField student = new JTextField(15);
        JSpinner rating = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextField content = new JTextField(25);
        Object[] fields = {"리뷰번호", id, "학번", student, "별점", rating, "후기 (선택)", content};

        if (confirm("리뷰 작성", fields)) {
            try {
                rating.commitEdit();
            } catch (java.text.ParseException e) {
                message("별점은 1부터 5까지 입력해 주세요.");
                return;
            }
            runDatabase(conn -> {
                repository.insertReview(conn, number(id, "리뷰번호"), required(student, "학번"),
                        mealMenuId, (Integer) rating.getValue(), content.getText().strip());
                fill(reviewTable, repository.findReviews(conn, mealMenuId));
            });
        }
    }

    private void loadPolls() {
        runDatabase(conn -> {
            fill(pollTable, repository.findPolls(conn));
            fill(candidateTable, List.of());
        });
    }

    private void loadCandidates() {
        fill(candidateTable, List.of());
        int row = pollTable.getSelectedRow();
        if (row >= 0) {
            Number id = idAt(pollTable, row);
            runDatabase(conn -> fill(candidateTable, repository.findCandidates(conn, id)));
        }
    }

    private void insertVote() {
        int pollRow = pollTable.getSelectedRow();
        int candidateRow = candidateTable.getSelectedRow();
        if (pollRow < 0 || candidateRow < 0) {
            message("투표회차와 후보메뉴를 차례로 선택해 주세요.");
            return;
        }

        Number pollId = idAt(pollTable, pollRow);
        Number menuId = idAt(candidateTable, candidateRow);
        JTextField id = new JTextField(15);
        JTextField student = new JTextField(15);
        Object[] fields = {"투표번호", id, "학번", student};

        if (confirm("투표하기", fields)) {
            runDatabase(conn -> {
                int inserted = repository.insertVote(conn, number(id, "투표번호"),
                        required(student, "학번"), pollId, menuId);
                if (inserted == 0) {
                    message("현재 진행 중인 투표의 후보에만 투표할 수 있습니다.");
                } else {
                    message("투표가 저장됐습니다.");
                }
                fill(candidateTable, repository.findCandidates(conn, pollId));
            });
        }
    }

    private void runDatabase(DatabaseAction action) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, password)) {
            action.run(conn);
        } catch (IllegalArgumentException e) {
            message(e.getMessage());
        } catch (SQLException e) {
            message("DB 작업 실패\n" + e.getMessage());
        }
    }

    private static int number(JTextField field, String label) {
        try {
            return Integer.parseInt(field.getText().strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "를 정수로 입력해 주세요.");
        }
    }

    private static String required(JTextField field, String label) {
        String value = field.getText().strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " 항목을 입력해 주세요.");
        }
        return value;
    }

    private static Number idAt(JTable table, int row) {
        return (Number) table.getValueAt(row, 0);
    }

    private static JTable table(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static void fill(JTable table, List<Object[]> rows) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private static JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(e -> action.run());
        return button;
    }

    private boolean confirm(String title, Object[] fields) {
        return JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION;
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    @FunctionalInterface
    private interface DatabaseAction {
        void run(Connection conn) throws SQLException;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JPasswordField input = new JPasswordField(15);
            int choice = JOptionPane.showConfirmDialog(
                    null, input, "Oracle 비밀번호 입력", JOptionPane.OK_CANCEL_OPTION
            );

            if (choice == JOptionPane.OK_OPTION) {
                MenuFrame frame = new MenuFrame(new String(input.getPassword()));
                input.setText("");
                frame.setVisible(true);
            }
        });
    }
}
