-- LÝ DO: Đây là bảng core của toàn hệ thống bán vé.
--        Workspace quản lý "không gian cố định", còn event ticketing
--        quản lý "sự kiện có thời hạn" — khái niệm hoàn toàn khác.
CREATE TABLE Events (
    event_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id      NUMBER NOT NULL,              -- FK -> EventCategories
    venue_id         NUMBER NOT NULL,              -- FK -> Venues
    organizer_id     NUMBER NOT NULL,              -- FK -> Employees (người phụ trách)

    event_name       VARCHAR2(300) NOT NULL,
    description      CLOB,
    banner_url       VARCHAR2(500),

    -- Thời gian diễn ra
    start_datetime   TIMESTAMP NOT NULL,
    end_datetime     TIMESTAMP NOT NULL,

    -- Thời gian mở/đóng bán vé
    sale_start       TIMESTAMP NOT NULL,
    sale_end         TIMESTAMP NOT NULL,

    -- Trạng thái vòng đời sự kiện
    status           VARCHAR2(20) DEFAULT 'DRAFT',
    -- DRAFT: Đang soạn thảo
    -- PUBLISHED: Đã đăng, chưa mở bán
    -- ON_SALE: Đang mở bán vé
    -- SOLD_OUT: Hết vé
    -- CANCELLED: Đã hủy
    -- COMPLETED: Đã diễn ra xong

    -- Snapshot capacity tại thời điểm tạo event
    -- (tránh conflict nếu venue thay đổi capacity sau này)
    total_capacity   NUMBER NOT NULL,

    -- Có sơ đồ ghế hay không (FALSE = standing/khu vực không đánh số ghế)
    is_seating_chart NUMBER(1) DEFAULT 0 CHECK (is_seating_chart IN (0, 1)),

    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted       NUMBER(1) DEFAULT 0 CHECK (is_deleted IN (0, 1))
);
