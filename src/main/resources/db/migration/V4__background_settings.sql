-- Page background: two pastel gradient colours, watermark strength and panel opacity.
-- Opacities are percentages (0-100). Existing settings rows get the defaults.
ALTER TABLE APP_CONFIG ADD (
    BG_COLOR_1          VARCHAR2(7) DEFAULT '#e6eef8' NOT NULL,
    BG_COLOR_2          VARCHAR2(7) DEFAULT '#fff1dc' NOT NULL,
    BG_PATTERN_OPACITY  NUMBER(3)   DEFAULT 10 NOT NULL,
    PANEL_OPACITY       NUMBER(3)   DEFAULT 90 NOT NULL
);
