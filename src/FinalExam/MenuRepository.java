package FinalExam;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;

public class MenuRepository {
    public ArrayList<Object[]> selectAll(Connection conn) throws SQLException {
        return query(conn, "SELECT menu_id, menu_name, menu_category FROM 메뉴 ORDER BY menu_id");
    }

    public void insert(Connection conn, Number id, String name, String category) throws SQLException {
        execute(conn, "INSERT INTO 메뉴 (menu_id, menu_name, menu_category) VALUES (?, ?, ?)",
                id, name, category);
    }

    public int update(Connection conn, Number id, String name, String category) throws SQLException {
        return execute(conn, "UPDATE 메뉴 SET menu_name = ?, menu_category = ? WHERE menu_id = ?",
                name, category, id);
    }

    public int delete(Connection conn, Number id) throws SQLException {
        return execute(conn, "DELETE FROM 메뉴 WHERE menu_id = ?", id);
    }

    public ArrayList<Object[]> findMeals(Connection conn, LocalDate date) throws SQLException {
        return query(conn, """
                SELECT sm.meal_plan_menu_id, TO_CHAR(s.serving_date, 'YYYY-MM-DD'),
                       s.meal_type, m.menu_name, sm.sale_price
                FROM 식단 s
                JOIN 식단메뉴 sm ON sm.meal_plan_id = s.meal_plan_id
                JOIN 메뉴 m ON m.menu_id = sm.menu_id
                WHERE s.serving_date >= ? AND s.serving_date < ?
                ORDER BY CASE s.meal_type WHEN '아침' THEN 1 WHEN '점심' THEN 2 ELSE 3 END,
                         sm.meal_plan_menu_id
                """, Date.valueOf(date), Date.valueOf(date.plusDays(1)));
    }

    public ArrayList<Object[]> findMealMenus(Connection conn) throws SQLException {
        return query(conn, """
                SELECT sm.meal_plan_menu_id, TO_CHAR(s.serving_date, 'YYYY-MM-DD'),
                       s.meal_type, m.menu_name
                FROM 식단메뉴 sm
                JOIN 식단 s ON s.meal_plan_id = sm.meal_plan_id
                JOIN 메뉴 m ON m.menu_id = sm.menu_id
                ORDER BY s.serving_date DESC, sm.meal_plan_menu_id
                """);
    }

    public ArrayList<Object[]> findReviews(Connection conn, Number mealMenuId) throws SQLException {
        return query(conn, """
                SELECT review_id, student_id, rating, review_content,
                       TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS')
                FROM 리뷰
                WHERE meal_plan_menu_id = ?
                ORDER BY created_at DESC, review_id DESC
                """, mealMenuId);
    }

    public void insertReview(Connection conn, Number id, String studentId, Number mealMenuId,
                             int rating, String content) throws SQLException {
        execute(conn, """
                INSERT INTO 리뷰 (review_id, student_id, meal_plan_menu_id, rating, review_content, created_at)
                VALUES (?, ?, ?, ?, ?, SYSDATE)
                """, id, studentId, mealMenuId, rating, content.isEmpty() ? null : content);
    }

    public ArrayList<Object[]> findPolls(Connection conn) throws SQLException {
        return query(conn, """
                SELECT poll_id, poll_name,
                       TO_CHAR(starts_at, 'YYYY-MM-DD HH24:MI:SS'),
                       TO_CHAR(ends_at, 'YYYY-MM-DD HH24:MI:SS'),
                       CASE WHEN SYSDATE < starts_at THEN '예정'
                            WHEN SYSDATE < ends_at THEN '진행 중' ELSE '종료' END
                FROM 투표회차
                ORDER BY poll_id
                """);
    }

    public ArrayList<Object[]> findCandidates(Connection conn, Number pollId) throws SQLException {
        return query(conn, """
                SELECT c.menu_id, m.menu_name,
                       CASE WHEN SYSDATE >= p.ends_at THEN (
                           SELECT COUNT(*)
                           FROM 투표 v
                           WHERE v.poll_id = c.poll_id AND v.menu_id = c.menu_id
                       ) END AS vote_count
                FROM 후보메뉴 c
                JOIN 메뉴 m ON m.menu_id = c.menu_id
                JOIN 투표회차 p ON p.poll_id = c.poll_id
                WHERE c.poll_id = ?
                ORDER BY vote_count DESC NULLS LAST, c.menu_id
                """, pollId);
    }

    public int insertVote(Connection conn, Number id, String studentId, Number pollId, Number menuId)
            throws SQLException {
        return execute(conn, """
                INSERT INTO 투표 (vote_id, student_id, poll_id, menu_id, voted_at)
                SELECT ?, ?, c.poll_id, c.menu_id, SYSDATE
                FROM 후보메뉴 c
                JOIN 투표회차 p ON p.poll_id = c.poll_id
                WHERE c.poll_id = ? AND c.menu_id = ?
                  AND SYSDATE >= p.starts_at AND SYSDATE < p.ends_at
                """, id, studentId, pollId, menuId);
    }

    private ArrayList<Object[]> query(Connection conn, String sql, Object... values)
            throws SQLException {
        ArrayList<Object[]> rows = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bind(pstmt, values);

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData columns = rs.getMetaData();
                int count = columns.getColumnCount();

                while (rs.next()) {
                    Object[] row = new Object[count];
                    for (int i = 0; i < count; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private int execute(Connection conn, String sql, Object... values) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bind(pstmt, values);
            return pstmt.executeUpdate();
        }
    }

    private void bind(PreparedStatement pstmt, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                pstmt.setNull(i + 1, Types.VARCHAR);
            } else {
                pstmt.setObject(i + 1, values[i]);
            }
        }
    }
}
