# Girvi Khata — Updated Product Blueprint

Date: 2026-08-01
Owner direction: practical, bilingual and shop-floor friendly
Current delivery branch: `agent/alpha25a-practical-entry`
Protected rollback base: `baseline/alpha21-owner-approved`

## 1. Product promise

Girvi Khata replaces the shopkeeper's diary plus calculator with one easy workflow that answers five questions instantly:

1. Customer ko total kitna paisa diya?
2. Kab-kab aur kitna additional paisa diya?
3. Customer ne principal aur interest mein kitna jama kiya?
4. Aaj tak principal aur interest kitna baki hai?
5. Aaj saman chhudane par total kitna lena hai?

The app must remain usable without internet for all core work.

## 2. Language and entry policy

- Every free-text field accepts Hindi, English and mixed Hindi-English Unicode text.
- UI supports bilingual labels first; later settings may offer Hindi, English or bilingual display.
- Contact-imported name and mobile remain editable.
- Search covers stored Unicode text, mobile, girvi number, item and date.
- Transliteration search is a later enhancement, not a release blocker.

## 3. Core navigation

### Home
- Naya Girvi / New Girvi
- Customers
- Active Girvi
- Today's collections
- Search
- Backup health

### Girvi detail
- Customer and item summary
- Photos
- Two-column ledger
- Add more amount
- Receive payment
- Calculate settlement
- Release/close pledge

## 4. New girvi workflow

### Customer
- Manual name
- Android Contact picker beside name/mobile
- Imported name and number fill together
- Both fields editable
- Existing-customer match and non-blocking duplicate warning
- Optional live customer photo

### Item
- One or more pledged items
- Category, item name, quantity and description
- Optional live item photo
- No gallery import in the initial secure implementation

### Weight
Simple mode:
- Value plus unit: gram, tola, kilogram, piece or custom

Advanced mode:
- Gross weight
- Deduction
- Auto-calculated net weight
- Purity/carat can be added later

### Date
- Calendar opens directly
- Today selected by default
- Back-date allowed
- Future date blocked
- Date is stored with the girvi and drives later calculations

### Money and interest preview
- Principal
- Monthly percentage in Alpha 25A compatibility mode
- Immediate monthly-interest preview
- Review screen before final save

## 5. Interest engine target — Alpha 25B

Supported modes:

1. Percentage per month
2. Flat amount per month
3. Compound interest as an advanced option

Period rules:
- Exact per day
- Full calendar month
- Partial month treated as full month
- Complete months plus remaining days
- Configurable compounding every 1/2/3/6 months or 1/2/3 years

Every girvi/advance stores its own immutable calculation rule so changing shop settings never silently changes old accounts.

## 6. Additional advances — Alpha 25C

Inside an active girvi:
- Add More Amount
- Amount and date
- Reuse previous interest rule or choose a separate rule
- Each advance calculates from its own date
- Confirmation must distinguish money given from payment received

Ledger presentation:

| Shopkeeper gave | Customer paid |
|---|---|
| Initial principal | Interest payment |
| Additional advance | Principal payment |
| Adjustment/reversal | Settlement payment |

Totals:
- Total advanced
- Principal returned
- Principal outstanding
- Interest received
- Interest outstanding
- Total due today

## 7. Payment and settlement — Alpha 25D

Payment types:
- Full settlement
- Interest only
- Principal only
- Partial payment
- Custom split

Allocation choices:
- Interest first
- Principal first
- Manual split

Settlement screen:
- User-selected settlement date
- Per-advance principal/interest calculation
- Flat, daily and compound comparison where applicable
- Discount/adjustment with mandatory reason
- Close only after confirmation or explicit owner override
- Immutable payment history; mistakes corrected by reversal

## 8. Side calculator

Available from amount, advance, payment and settlement screens:
- Add, subtract, multiply, divide
- Percentage
- Monthly interest
- Daily interest
- Insert result into active field
- Opening calculator must not clear the form

## 9. Security

- Six-digit owner PIN
- Optional fingerprint unlock
- Auto-lock after configured timeout
- Practical-entry shortcut requires owner authentication
- Encrypted app-private business records
- Verified write coordinator for business mutations
- Non-exported internal activities
- No WhatsApp login, backup or recovery in the approved roadmap

## 10. Photo and media policy

- Live camera capture only for first release
- Customer and item photos optional
- Files remain app-private and never appear in public gallery
- Failed/cancelled capture cleans temporary files
- Media becomes part of encrypted backup before stable promotion
- Orphan-media cleanup must be implemented before long-term production use

## 11. Offline-first data policy

Core operations must work offline:
- Customer and girvi entry
- Interest calculation
- Additional advances
- Payments
- Settlement
- Search and reports
- Manual encrypted backup export

Internet-dependent functions are optional later additions only.

## 12. UX rules

- Essential fields visible first
- Advanced interest and weight options collapsed by default
- Bilingual plain-language labels
- Large tap targets
- No silent financial recalculation
- Review before saving major entries
- Clear distinction between money given and money received
- Every destructive/financial correction uses confirmation and audit evidence

## 13. Release definition

A build is not stable merely because it compiles. Stable promotion requires:
- Green unit tests and compilation
- Correct permanent test certificate
- APK integrity verification
- Old encrypted snapshot compatibility
- Entry, payment, settlement and restore phone tests
- Photo capture and backup/restore test
- Owner approval
- Rollback package and backup already available

## 14. Explicit non-goals for current Alpha 25A

- WhatsApp authentication or backup
- Cloud account dependency
- Automatic Google Drive backup
- Compound/flat/per-day engine
- Additional advance ledger
- Full settlement redesign
- PDF receipt and Bluetooth printing

These remain sequenced work, not forgotten work.
