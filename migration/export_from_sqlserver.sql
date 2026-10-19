/* =====================================================================
   Export InventorySysDB (SQL Server) as Oracle INSERT statements.

   HOW TO USE
   1. Start the Java app once against an empty Oracle schema so Flyway
      creates the tables (or run V1__schema.sql and V2__seed.sql by hand).
   2. In SSMS, open this script against InventorySysDB.
      Query > Results To > Results to Text  (Ctrl+T)
      Tools > Options > Query Results > SQL Server > Results to Text:
          set "Maximum number of characters displayed in each column" to 8192
          and untick "Include column headers".
   3. Run it (needs SQL Server 2016 or later for STRING_SPLIT).
   4. Save the output as import.sql and run it in SQL Developer or SQL*Plus
      as the application's Oracle user. It ends with COMMIT.

   WHAT IT CLEANS UP
   - Amounts stored as text like '1,200' become numbers.
   - Dates are converted whether they were datetime columns or text.
   - Plain-text passwords get a {noop} prefix; each is re-hashed with
     BCrypt automatically the first time that user logs in.
   - PClasses.Products ('A,B,C') becomes PRODUCT.PRODUCT_CLASS_ID.
   - CClass's one-column-per-class layout becomes CLIENT_CLASS_DISCOUNT rows.
   - Purchases, Sales and Offers go into ORDER_LINE with an ORDER_TYPE.
   ===================================================================== */
SET NOCOUNT ON;

PRINT 'SET DEFINE OFF';
PRINT 'ALTER SESSION SET NLS_NUMERIC_CHARACTERS = ''.,'';';
PRINT '-- Remove the seeded admin account; the imported users (with their own administrators) replace it.';
PRINT 'DELETE FROM APP_USER WHERE USERNAME = ''admin'';';
PRINT '';

/* ---------------------------------------------------------------- Users */
PRINT '-- Users';
SELECT N'INSERT INTO APP_USER (FIRST_NAME, LAST_NAME, EMAIL, USERNAME, PASSWORD, USER_ROLE, LAST_LOGIN_AT, LAST_PASSWORD_CHANGE_AT) VALUES ('
     + ISNULL(N'''' + REPLACE(CAST(u.Name AS nvarchar(max)), N'''', N'''''') + N'''', N'''-''') + N', '
     + ISNULL(N'''' + REPLACE(CAST(u.Lastname AS nvarchar(max)), N'''', N'''''') + N'''', N'''-''') + N', '
     + ISNULL(N'''' + REPLACE(CAST(u.Email AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + N'''' + REPLACE(CAST(u.Username AS nvarchar(max)), N'''', N'''''') + N''', '
     + N'''{noop}' + REPLACE(CAST(u.Password AS nvarchar(max)), N'''', N'''''') + N''', '
     + CASE WHEN CAST(u.UserRole AS nvarchar(10)) = N'1' THEN N'1' ELSE N'2' END + N', '
     + ISNULL(N'TIMESTAMP ''' + CONVERT(nvarchar(19), d.LastLogin, 120) + N'''', N'NULL') + N', '
     + ISNULL(N'TIMESTAMP ''' + CONVERT(nvarchar(19), d.LastPwd, 120) + N'''', N'NULL')
     + N');'
FROM Users u
CROSS APPLY (SELECT
    COALESCE(TRY_CONVERT(datetime2, CAST(u.LastLoginDate AS nvarchar(50))), TRY_CONVERT(datetime2, CAST(u.LastLoginDate AS nvarchar(50)), 103)) AS LastLogin,
    COALESCE(TRY_CONVERT(datetime2, CAST(u.LastPasswordChanged AS nvarchar(50))), TRY_CONVERT(datetime2, CAST(u.LastPasswordChanged AS nvarchar(50)), 103)) AS LastPwd
) d;

/* ------------------------------------------- Merchants, Companies, Clients */
PRINT '-- Merchants';
SELECT N'INSERT INTO MERCHANT (NAME, TIN, ADDRESS, EMAIL, PHONE) VALUES ('
     + N'''' + REPLACE(CAST(MerchantName AS nvarchar(max)), N'''', N'''''') + N''', '
     + ISNULL(N'''' + REPLACE(CAST(MerchantTIN AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(MerchantAddress AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(MerchantMail AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(MerchantNumber AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N');'
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY MerchantName ORDER BY (SELECT 0)) AS rn FROM Merchants) x
WHERE rn = 1;  -- names are unique in Oracle; duplicates keep the first row

PRINT '-- Companies';
SELECT N'INSERT INTO COMPANY (NAME, TIN, ADDRESS, EMAIL, PHONE) VALUES ('
     + N'''' + REPLACE(CAST(CompanyName AS nvarchar(max)), N'''', N'''''') + N''', '
     + ISNULL(N'''' + REPLACE(CAST(CompanyTIN AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(CompanyAddress AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(CompanyMail AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(CompanyNumber AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N');'
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY CompanyName ORDER BY (SELECT 0)) AS rn FROM Companies) x
WHERE rn = 1;

PRINT '-- Clients';
SELECT N'INSERT INTO CLIENT (NAME, TIN, ADDRESS, EMAIL, PHONE) VALUES ('
     + N'''' + REPLACE(CAST(ClientName AS nvarchar(max)), N'''', N'''''') + N''', '
     + ISNULL(N'''' + REPLACE(CAST(ClientTIN AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientAddress AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientMail AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientNumber AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N');'
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY ClientName ORDER BY (SELECT 0)) AS rn FROM Clients) x
WHERE rn = 1;

PRINT '-- NotClients (offer recipients who are not clients) -> PROSPECT, latest row per name';
SELECT N'INSERT INTO PROSPECT (NAME, TIN, ADDRESS, EMAIL, PHONE, REGISTERED_AT) VALUES ('
     + N'''' + REPLACE(CAST(ClientName AS nvarchar(max)), N'''', N'''''') + N''', '
     + ISNULL(N'''' + REPLACE(CAST(ClientTIN AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientAddress AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientMail AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'''' + REPLACE(CAST(ClientNumber AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + ISNULL(N'TIMESTAMP ''' + CONVERT(nvarchar(19), TRY_CONVERT(datetime2, CAST(ClientRegisteredDate AS nvarchar(50))), 120) + N'''', N'SYSTIMESTAMP')
     + N');'
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY ClientName ORDER BY ClientRegisteredDate DESC) AS rn FROM NotClients) x
WHERE rn = 1;

/* --------------------------------------------------------- Product classes */
PRINT '-- Product classes (PClasses)';
SELECT N'INSERT INTO PRODUCT_CLASS (NAME) VALUES (''' + REPLACE(LTRIM(RTRIM(CAST(ClassName AS nvarchar(max)))), N'''', N'''''') + N''');'
FROM (SELECT DISTINCT ClassName FROM PClasses WHERE ClassName IS NOT NULL) x;

/* ---------------------------------------------------------------- Products */
PRINT '-- Products';
SELECT N'INSERT INTO PRODUCT (NAME, UNIT, BUY_PRICE, SALE_PRICE, QUANTITY_LIMIT) VALUES ('
     + N'''' + REPLACE(CAST(p.productname AS nvarchar(max)), N'''', N'''''') + N''', '
     + N'''' + REPLACE(ISNULL(CAST(p.productunit AS nvarchar(max)), N'pcs'), N'''', N'''''') + N''', '
     + ISNULL(CONVERT(nvarchar(40), TRY_CONVERT(decimal(14,2), REPLACE(CAST(p.productbuyprice AS nvarchar(50)), N',', N''))), N'0') + N', '
     + ISNULL(CONVERT(nvarchar(40), TRY_CONVERT(decimal(14,2), REPLACE(CAST(p.productsaleprice AS nvarchar(50)), N',', N''))), N'0') + N', '
     + ISNULL(CONVERT(nvarchar(20), TRY_CONVERT(int, REPLACE(CAST(p.productquantitylimit AS nvarchar(50)), N',', N''))), N'0')
     + N');'
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY productname ORDER BY (SELECT 0)) AS rn FROM Products) p
WHERE rn = 1;

PRINT '-- Which class each product belongs to (from PClasses.Products, a comma-separated list)';
PRINT '-- If a product was listed in several classes, the last statement wins.';
SELECT N'UPDATE PRODUCT SET PRODUCT_CLASS_ID = (SELECT ID FROM PRODUCT_CLASS WHERE NAME = '''
     + REPLACE(LTRIM(RTRIM(CAST(c.ClassName AS nvarchar(max)))), N'''', N'''''')
     + N''') WHERE NAME = ''' + REPLACE(LTRIM(RTRIM(s.value)), N'''', N'''''') + N''';'
FROM PClasses c
CROSS APPLY STRING_SPLIT(CAST(c.Products AS nvarchar(max)), N',') s
WHERE LTRIM(RTRIM(s.value)) <> N'';

/* ------------------------------------------------ Client discounts (CClass) */
PRINT '-- Client discounts: every CClass column other than Id/ClientName/RegisteredDate is a class';
DECLARE @cols nvarchar(max), @sql nvarchar(max);
SELECT @cols = STRING_AGG(N'(' + QUOTENAME(name, N'''') + N', CAST(' + QUOTENAME(name) + N' AS nvarchar(50)))', N', ')
FROM sys.columns
WHERE object_id = OBJECT_ID(N'dbo.CClass') AND name NOT IN (N'Id', N'ClientName', N'RegisteredDate');

IF @cols IS NOT NULL
BEGIN
    -- NCHAR(39) is a single quote; it keeps the quoting readable.
    SET @sql = N'
    SELECT N''INSERT INTO CLIENT_CLASS_DISCOUNT (CLIENT_ID, PRODUCT_CLASS_ID, DISCOUNT_PERCENT) SELECT c.ID, k.ID, ''
         + CONVERT(nvarchar(20), TRY_CONVERT(decimal(5,2), v.val))
         + N'' FROM CLIENT c, PRODUCT_CLASS k WHERE c.NAME = ''
         + NCHAR(39) + REPLACE(CAST(cc.ClientName AS nvarchar(max)), NCHAR(39), NCHAR(39) + NCHAR(39)) + NCHAR(39)
         + N'' AND k.NAME = ''
         + NCHAR(39) + REPLACE(v.cls, NCHAR(39), NCHAR(39) + NCHAR(39)) + NCHAR(39)
         + N'';''
    FROM dbo.CClass cc
    CROSS APPLY (VALUES ' + @cols + N') v(cls, val)
    WHERE TRY_CONVERT(decimal(5,2), v.val) > 0;';
    EXEC sp_executesql @sql;
END

/* ----------------------------------------------------------- Settings */
PRINT '-- Settings (Configurations). The TIN column held the VAT percentage.';
SELECT TOP 1 N'UPDATE APP_CONFIG SET CURRENCY = '
     + ISNULL(N'''' + REPLACE(CAST(Currency AS nvarchar(10)), N'''', N'''''') + N'''', N'''ALL''')
     + N', VAT_PERCENT = ' + ISNULL(CONVERT(nvarchar(20), TRY_CONVERT(decimal(5,2), REPLACE(CAST(TIN AS nvarchar(20)), N',', N'.'))), N'20')
     + N', BRAND_RGB = ' + ISNULL(N'''' + REPLACE(CAST(RGB AS nvarchar(20)), N' ', N'') + N'''', N'''11,71,121''')
     + N', LOGO_FILE = ' + ISNULL(NULLIF(N'''' + REPLACE(CAST(LogoInvoice AS nvarchar(200)), N'''', N'''''') + N'''', N''''''), N'NULL')
     + N' WHERE ID = 1;'
FROM Configurations ORDER BY Id;

/* -------------------------------------------- Purchases, Sales, Offers */
PRINT '-- Order lines';
WITH src AS (
    SELECT 'PURCHASE' AS t, CAST(PurchaseCode AS nvarchar(20)) AS code, [Date], Product, Provider, Buyer, Unit, Amount, Price,
           ValuenoVAT, VATValue, ValuewithVAT, CAST(NULL AS nvarchar(10)) AS Percentage, Usern, LastUpdated, CAST(NULL AS nvarchar(5)) AS ExistingClient
    FROM Purchases
    UNION ALL
    SELECT 'SALE', CAST(SaleCode AS nvarchar(20)), [Date], Product, Provider, Buyer, Unit, Amount, Price,
           ValuenoVAT, VATValue, ValuewithVAT, CAST(Percentage AS nvarchar(10)), Usern, LastUpdated, NULL
    FROM Sales
    UNION ALL
    SELECT 'OFFER', CAST(OfferCode AS nvarchar(20)), [Date], Product, Provider, Buyer, Unit, Amount, Price,
           ValuenoVAT, VATValue, ValuewithVAT, CAST(Percentage AS nvarchar(10)), Usern, LastUpdated, CAST(ExistingClient AS nvarchar(5))
    FROM Offers
)
SELECT N'INSERT INTO ORDER_LINE (ORDER_TYPE, ORDER_CODE, CREATED_AT, PRODUCT_NAME, PROVIDER_NAME, BUYER_NAME, UNIT, QUANTITY, UNIT_PRICE, DISCOUNT_PERCENT, VALUE_NO_VAT, VAT_VALUE, VALUE_WITH_VAT, CREATED_BY, EXISTING_CLIENT, LAST_UPDATED) VALUES ('
     + N'''' + t + N''', '
     + N'''' + REPLACE(code, N'''', N'''''') + N''', '
     + ISNULL(N'TIMESTAMP ''' + CONVERT(nvarchar(19), d.created, 120) + N'''', N'SYSTIMESTAMP') + N', '
     + N'''' + REPLACE(ISNULL(CAST(Product AS nvarchar(max)), N'?'), N'''', N'''''') + N''', '
     + N'''' + REPLACE(ISNULL(CAST(Provider AS nvarchar(max)), N'?'), N'''', N'''''') + N''', '
     + N'''' + REPLACE(ISNULL(CAST(Buyer AS nvarchar(max)), N'?'), N'''', N'''''') + N''', '
     + ISNULL(N'''' + REPLACE(CAST(Unit AS nvarchar(max)), N'''', N'''''') + N'''', N'NULL') + N', '
     + CONVERT(nvarchar(20), CASE WHEN n.qty > 0 THEN n.qty ELSE 1 END) + N', '
     + CONVERT(nvarchar(40), ISNULL(n.price, 0)) + N', '
     + CONVERT(nvarchar(20), ISNULL(n.pct, 0)) + N', '
     + CONVERT(nvarchar(40), ISNULL(n.novat, 0)) + N', '
     + CONVERT(nvarchar(40), ISNULL(n.vat, 0)) + N', '
     + CONVERT(nvarchar(40), ISNULL(n.withvat, ISNULL(n.novat, 0) + ISNULL(n.vat, 0))) + N', '
     + N'''' + REPLACE(ISNULL(CAST(Usern AS nvarchar(max)), N'unknown'), N'''', N'''''') + N''', '
     + CASE WHEN t <> 'OFFER' THEN N'NULL'
            WHEN UPPER(LTRIM(RTRIM(ExistingClient))) = N'YES' THEN N'''Y'''
            ELSE N'''N''' END + N', '
     + ISNULL(N'TIMESTAMP ''' + CONVERT(nvarchar(19), d.updated, 120) + N'''', N'NULL')
     + N');'
FROM src
CROSS APPLY (SELECT
    COALESCE(TRY_CONVERT(datetime2, CAST([Date] AS nvarchar(50))), TRY_CONVERT(datetime2, CAST([Date] AS nvarchar(50)), 103)) AS created,
    COALESCE(TRY_CONVERT(datetime2, CAST(LastUpdated AS nvarchar(50))), TRY_CONVERT(datetime2, CAST(LastUpdated AS nvarchar(50)), 103)) AS updated
) d
CROSS APPLY (SELECT
    TRY_CONVERT(int, REPLACE(CAST(Amount AS nvarchar(50)), N',', N'')) AS qty,
    TRY_CONVERT(decimal(14,2), REPLACE(CAST(Price AS nvarchar(50)), N',', N'')) AS price,
    TRY_CONVERT(decimal(5,2), REPLACE(CAST(Percentage AS nvarchar(20)), N',', N'.')) AS pct,
    TRY_CONVERT(decimal(16,2), REPLACE(CAST(ValuenoVAT AS nvarchar(50)), N',', N'')) AS novat,
    TRY_CONVERT(decimal(16,2), REPLACE(CAST(VATValue AS nvarchar(50)), N',', N'')) AS vat,
    TRY_CONVERT(decimal(16,2), REPLACE(CAST(ValuewithVAT AS nvarchar(50)), N',', N'')) AS withvat
) n;

/* -------------------------------------------- Order numbers */
PRINT '-- New orders are numbered 00000001, 00000002, ... and skip any imported code.';
PRINT 'COMMIT;';
