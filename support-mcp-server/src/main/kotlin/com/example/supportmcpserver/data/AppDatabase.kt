package com.example.supportmcpserver.data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class AppDatabase(dataDir: File) {

    val connection: Connection = DriverManager.getConnection(
        "jdbc:sqlite:${File(dataDir, "support.db").absolutePath}"
    ).apply {
        createStatement().use { it.executeUpdate("PRAGMA journal_mode=WAL") }
    }

    init {
        createSchema()
        seedFaq()
        seedUsers()
        seedTickets()
    }

    private fun createSchema() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS faq (
                    id       TEXT PRIMARY KEY,
                    question TEXT NOT NULL,
                    answer   TEXT NOT NULL,
                    tags     TEXT NOT NULL
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id         TEXT PRIMARY KEY,
                    name       TEXT NOT NULL,
                    email      TEXT NOT NULL,
                    plan       TEXT NOT NULL,
                    status     TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    location   TEXT NOT NULL
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS tickets (
                    id          TEXT PRIMARY KEY,
                    user_id     TEXT NOT NULL,
                    subject     TEXT NOT NULL,
                    description TEXT NOT NULL,
                    status      TEXT NOT NULL,
                    priority    TEXT NOT NULL,
                    created_at  TEXT NOT NULL,
                    updated_at  TEXT NOT NULL,
                    resolution  TEXT
                )
                """.trimIndent()
            )
        }
    }

    private fun seedFaq() {
        val count = connection.prepareStatement("SELECT COUNT(*) FROM faq").use {
            it.executeQuery().getInt(1)
        }
        if (count > 0) return

        val sql = "INSERT INTO faq (id, question, answer, tags) VALUES (?,?,?,?)"
        connection.prepareStatement(sql).use { ps ->
            listOf(
                arrayOf(
                    "faq-001",
                    "How do I reset my password?",
                    "Go to the login page and click 'Forgot Password'. Enter your email address and we will send you a reset link within 5 minutes. Check your spam folder if you don't see it. The link expires after 24 hours.",
                    "password,reset,login,auth,authentication,account,forgot"
                ),
                arrayOf(
                    "faq-002",
                    "How do I cancel my subscription?",
                    "To cancel your subscription, go to Account Settings > Billing > Cancel Subscription. Your access will continue until the end of the current billing period. No refunds are issued for partial months.",
                    "cancel,subscription,billing,refund,payment,unsubscribe"
                ),
                arrayOf(
                    "faq-003",
                    "What payment methods do you accept?",
                    "We accept Visa, Mastercard, American Express, and PayPal. All payments are processed securely through Stripe. We do not store your card details on our servers.",
                    "payment,billing,card,credit,stripe,paypal,visa,mastercard,amex"
                ),
                arrayOf(
                    "faq-004",
                    "How do I change my email address?",
                    "Go to Account Settings > Profile > Edit Email. You will need to verify your new email address before the change takes effect. Your old email will receive a notification about the change.",
                    "email,account,profile,change,update,address"
                ),
                arrayOf(
                    "faq-005",
                    "Can I use the app on multiple devices?",
                    "Yes, your account can be used on up to 3 devices simultaneously. You can manage your active sessions in Account Settings > Security > Active Sessions and log out from any device remotely.",
                    "devices,sessions,multiple,account,security,simultaneous"
                ),
                arrayOf(
                    "faq-006",
                    "How do I export my data?",
                    "Navigate to Account Settings > Privacy > Export Data. You can export your data in JSON or CSV format. The export will be emailed to your registered address within 24 hours.",
                    "export,data,privacy,download,gdpr,csv,json,personal"
                ),
                arrayOf(
                    "faq-007",
                    "Why was I charged twice?",
                    "Duplicate charges sometimes appear as a pending authorization that is later cancelled automatically. If the duplicate charge persists after 5 business days, please open a support ticket with your bank statement screenshot and we will issue a full refund.",
                    "billing,charge,duplicate,refund,payment,charged,twice,double"
                ),
                arrayOf(
                    "faq-008",
                    "How do I enable two-factor authentication?",
                    "Go to Account Settings > Security > Two-Factor Authentication. We support authenticator apps (Google Authenticator, Authy) and SMS verification. Authenticator apps are recommended for better security.",
                    "2fa,two-factor,authentication,security,auth,login,totp,sms"
                ),
                arrayOf(
                    "faq-009",
                    "What happens to my data when I delete my account?",
                    "When you delete your account, all personal data is permanently deleted within 30 days per our Privacy Policy. Some anonymized usage data may be retained for analytics. You can request an immediate deletion by contacting support.",
                    "delete,account,data,privacy,gdpr,personal,remove,close"
                ),
                arrayOf(
                    "faq-010",
                    "How do I upgrade my plan?",
                    "Go to Account Settings > Billing > Change Plan. Upgrades take effect immediately and you are charged a prorated amount for the remaining billing period. Downgrades take effect at the next billing cycle.",
                    "upgrade,plan,billing,subscription,downgrade,pricing,pro,enterprise"
                ),
                arrayOf(
                    "faq-011",
                    "Why is my two-factor authentication code being rejected?",
                    "If your 2FA code is being rejected, the most common cause is clock drift — your device clock is out of sync. TOTP codes are time-based and valid only for 30 seconds. Fix: go to your device settings and enable automatic time synchronization (Settings > Date & Time > Set Automatically). After syncing, try logging in again. If the problem persists, use your backup codes or contact support to reset your 2FA.",
                    "2fa,two-factor,authentication,code,rejected,invalid,not working,totp,clock,time,sync,authenticator"
                )
            ).forEach { row ->
                row.forEachIndexed { i, v -> ps.setString(i + 1, v) }
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun seedUsers() {
        val count = connection.prepareStatement("SELECT COUNT(*) FROM users").use {
            it.executeQuery().getInt(1)
        }
        if (count > 0) return

        val sql = "INSERT INTO users (id, name, email, plan, status, created_at, location) VALUES (?,?,?,?,?,?,?)"
        connection.prepareStatement(sql).use { ps ->
            listOf(
                arrayOf("usr-001", "Alice Johnson",  "alice.johnson@example.com",  "pro",        "active",    "2023-01-15", "New York, USA"),
                arrayOf("usr-002", "Bob Smith",      "bob.smith@example.com",      "free",       "active",    "2023-03-22", "London, UK"),
                arrayOf("usr-003", "Carol Williams", "carol.williams@example.com", "enterprise", "active",    "2022-11-08", "Toronto, Canada"),
                arrayOf("usr-004", "David Brown",    "david.brown@example.com",    "pro",        "suspended", "2023-05-30", "Sydney, Australia"),
                arrayOf("usr-005", "Eva Martinez",   "eva.martinez@example.com",   "free",       "active",    "2024-01-10", "Madrid, Spain")
            ).forEach { row ->
                row.forEachIndexed { i, v -> ps.setString(i + 1, v) }
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun seedTickets() {
        val count = connection.prepareStatement("SELECT COUNT(*) FROM tickets").use {
            it.executeQuery().getInt(1)
        }
        if (count > 0) return

        val sql = """
            INSERT INTO tickets (id, user_id, subject, description, status, priority, created_at, updated_at, resolution)
            VALUES (?,?,?,?,?,?,?,?,?)
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            listOf(
                arrayOf(
                    "tkt-001", "usr-001", "Cannot log in after password reset",
                    "I reset my password using the email link but still cannot log in. Getting 'invalid credentials' error even with the new password.",
                    "resolved", "high", "2024-03-10", "2024-03-11",
                    "Account was locked due to too many failed login attempts. Account unlocked manually. User confirmed login works with the new password."
                ),
                arrayOf(
                    "tkt-002", "usr-001", "Billed twice in March",
                    "I see two charges of \$29.99 on my bank statement for March 2024. Only one charge should have been made.",
                    "resolved", "high", "2024-03-20", "2024-03-21",
                    "Duplicate charge confirmed via Stripe dashboard. Refund of \$29.99 issued. Will appear in 3-5 business days."
                ),
                arrayOf(
                    "tkt-003", "usr-002", "Feature request: dark mode",
                    "Would love to have a dark mode option in the app settings. It would be easier on the eyes during night-time use.",
                    "open", "low", "2024-04-01", "2024-04-01", null
                ),
                arrayOf(
                    "tkt-004", "usr-003", "Data export not received",
                    "Requested data export 3 days ago through Account Settings > Privacy > Export Data but still haven't received the email.",
                    "in_progress", "medium", "2024-04-05", "2024-04-07", null
                ),
                arrayOf(
                    "tkt-005", "usr-003", "2FA code not working",
                    "My Google Authenticator codes are being rejected at login. I've tried multiple times but keep getting 'invalid code' error.",
                    "resolved", "high", "2024-02-14", "2024-02-15",
                    "Clock drift on user device was causing TOTP codes to be invalid (off by more than 30 seconds). Instructed user to enable automatic time sync on their device. Issue resolved."
                ),
                arrayOf(
                    "tkt-006", "usr-004", "Account suspended without notice",
                    "My account was suddenly suspended. I have not violated any terms of service and need access restored urgently.",
                    "in_progress", "high", "2024-04-08", "2024-04-09", null
                ),
                arrayOf(
                    "tkt-007", "usr-005", "How to upgrade to Pro?",
                    "I want to upgrade from free to pro plan but the upgrade button is greyed out in billing settings.",
                    "resolved", "low", "2024-03-25", "2024-03-25",
                    "User needed to add a payment method first before upgrading. Guided through Account Settings > Billing > Add Payment Method. User successfully upgraded to Pro."
                ),
                arrayOf(
                    "tkt-008", "usr-002", "App crashes on Android 14",
                    "The app crashes immediately on launch on my Pixel 8 running Android 14. Happens every time, tried reinstalling.",
                    "open", "high", "2024-04-10", "2024-04-10", null
                )
            ).forEach { row ->
                row.forEachIndexed { i, v -> ps.setString(i + 1, v) }
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }
}
