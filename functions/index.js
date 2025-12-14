/**
 * Firebase Cloud Functions for CKL Banking
 * - OTP Email Service (SMTP)
 * - VNPay Payment Processing
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const crypto = require("crypto");

admin.initializeApp();

// ============================================
// Email Configuration (SMTP)
// ============================================
// Gmail SMTP Configuration
// TODO: Nên dùng Firebase Config để bảo mật hơn:
// firebase functions:config:set email.gmail_user="your-email@gmail.com"
// firebase functions:config:set email.gmail_pass="your-app-password"

const GMAIL_USER = "ledoananhhao2020@gmail.com";
const GMAIL_PASS = "hunulevwqyvwafpo"; // Gmail App Password

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: GMAIL_USER,
    pass: GMAIL_PASS,
  },
});

// ============================================
// VNPay Configuration
// ============================================
// TODO: Cấu hình VNPay credentials
// firebase functions:config:set vnpay.tmn_code="YOUR_TMN_CODE"
// firebase functions:config:set vnpay.secret_key="YOUR_SECRET_KEY"
// firebase functions:config:set vnpay.url="https://sandbox.vnpayment.vn/..."

const VNPAY_CONFIG = {
  tmnCode: process.env.VNPAY_TMN_CODE || "NPDDX09V",
  secretKey: process.env.VNPAY_SECRET_KEY ||
    "9AYFWXJQX0IZIL034I9IUPEWV54UA36B",
  url: process.env.VNPAY_URL ||
    "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
  returnUrl: process.env.VNPAY_RETURN_URL ||
    "https://your-domain.com/vnpay-return",
};

// ============================================
// Cloud Function: Send OTP Email
// ============================================
exports.sendOTPEmail = functions.firestore
    .onDocumentCreated("email_requests/{requestId}", async (event) => {
      const snap = event.data;
      const context = event.params;
      const emailData = snap.data();

      // Only process if status is pending
      if (emailData.status !== "pending") {
        functions.logger.log("Email request already processed, skipping...");
        return null;
      }

      const toEmail = emailData.toEmail;
      const otpCode = emailData.otpCode;
      const transactionId = emailData.transactionId;
      const subject = emailData.subject ||
        "Mã OTP xác thực - CKL Banking";

      if (!toEmail || !otpCode) {
        functions.logger.error("Missing email or OTP code");
        await snap.ref.update({
          status: "failed",
          error: "Missing email or OTP code",
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return null;
      }

      // Email HTML template
      const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Mã OTP - CKL Banking</title>
      </head>
      <body style="margin: 0; padding: 0; font-family: Arial, sans-serif;
        background-color: #f5f5f5;">
        <table role="presentation" style="width: 100%;
          border-collapse: collapse;">
          <tr>
            <td style="padding: 20px 0; text-align: center;
              background-color: #1976D2;">
              <h1 style="color: #ffffff; margin: 0; font-size: 24px;">
                CKL Banking
              </h1>
            </td>
          </tr>
          <tr>
            <td style="padding: 40px 20px; background-color: #ffffff;">
              <table role="presentation" style="width: 100%;
                max-width: 600px; margin: 0 auto;">
                <tr>
                  <td>
                    <h2 style="color: #333333; margin: 0 0 20px 0;">
                      Xác thực giao dịch
                    </h2>
                    <p style="color: #666666; font-size: 16px;
                      line-height: 1.5; margin: 0 0 20px 0;">
                      Xin chào,
                    </p>
                    <p style="color: #666666; font-size: 16px;
                      line-height: 1.5; margin: 0 0 30px 0;">
                      Mã OTP của bạn để xác thực giao dịch
                      <strong>${transactionId}</strong> là:
                    </p>
                    
                    <!-- OTP Code Box -->
                    <div style="background: linear-gradient(135deg,
                      #667eea 0%, #764ba2 100%); padding: 30px;
                      text-align: center; border-radius: 10px;
                      margin: 30px 0;">
                      <h1 style="color: #ffffff; font-size: 36px;
                        letter-spacing: 8px; margin: 0; font-weight: bold;
                        font-family: 'Courier New', monospace;">
                        ${otpCode}
                      </h1>
                    </div>
                    
                    <p style="color: #ff6b6b; font-size: 14px;
                      font-weight: bold; margin: 20px 0;">
                      ⏰ Mã OTP này có hiệu lực trong
                      <strong>2 phút</strong>
                    </p>
                    
                    <div style="background-color: #fff3cd;
                      border-left: 4px solid #ffc107; padding: 15px;
                      margin: 30px 0; border-radius: 4px;">
                      <p style="color: #856404; margin: 0; font-size: 14px;">
                        <strong>⚠️ Lưu ý bảo mật:</strong><br>
                        • Không chia sẻ mã OTP với bất kỳ ai<br>
                        • CKL Banking sẽ không bao giờ yêu cầu bạn
                        cung cấp mã OTP qua điện thoại<br>
                        • Nếu bạn không yêu cầu mã này,
                        vui lòng bỏ qua email này
                      </p>
                    </div>
                    
                    <hr style="border: none; border-top: 1px solid #eeeeee;
                      margin: 30px 0;">
                    
                    <p style="color: #999999; font-size: 12px; margin: 0;
                      text-align: center;">
                      Đây là email tự động,
                      vui lòng không trả lời email này.<br>
                      © ${new Date().getFullYear()} CKL Banking.
                      All rights reserved.
                    </p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>
      </body>
      </html>
    `;

      const mailOptions = {
        from: `CKL Banking <${GMAIL_USER}>`,
        to: toEmail,
        subject: subject,
        html: htmlContent,
        text: `Mã OTP của bạn để xác thực giao dịch ${transactionId}
          là: ${otpCode}. Mã này có hiệu lực trong 2 phút.`,
      };

      functions.logger.log("Preparing to send email:", {
        from: GMAIL_USER,
        to: toEmail,
        transactionId: transactionId,
      });

      try {
        // Send email
        const info = await transporter.sendMail(mailOptions);
        functions.logger.log("Email sent successfully:", info.messageId);

        // Update email request status
        await snap.ref.update({
          status: "sent",
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          messageId: info.messageId,
        });

        // Update OTP document status
        await admin.firestore()
            .collection("otps")
            .doc(transactionId)
            .update({
              status: "sent",
              emailSentAt: admin.firestore.FieldValue.serverTimestamp(),
            });

        return null;
      } catch (error) {
        functions.logger.error("Error sending email:", error);

        // Update status to failed
        await snap.ref.update({
          status: "failed",
          error: error.message,
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Update OTP document status
        await admin.firestore()
            .collection("otps")
            .doc(transactionId)
            .update({
              status: "failed",
              error: error.message,
            });

        return null;
      }
    });

// ============================================
// Cloud Function: Resend OTP Email
// ============================================
exports.resendOTPEmail = functions.https.onCall(async (data, context) => {
  // Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated",
    );
  }

  const transactionId = data.transactionId;
  const userId = context.auth.uid;

  if (!transactionId) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Transaction ID is required",
    );
  }

  try {
    // Get OTP document
    const otpDoc = await admin.firestore()
        .collection("otps")
        .doc(transactionId)
        .get();

    if (!otpDoc.exists) {
      throw new functions.https.HttpsError("not-found", "OTP not found");
    }

    const otpData = otpDoc.data();

    // Verify user owns this OTP
    if (otpData.userId !== userId) {
      throw new functions.https.HttpsError(
          "permission-denied",
          "Not authorized",
      );
    }

    // Check if OTP is already used
    if (otpData.isUsed) {
      throw new functions.https.HttpsError(
          "failed-precondition",
          "OTP already used",
      );
    }

    // Create new email request
    await admin.firestore()
        .collection("email_requests")
        .doc(transactionId + "_resend_" + Date.now())
        .set({
          transactionId: transactionId,
          toEmail: otpData.userEmail,
          otpCode: otpData.otpCode,
          subject: "Mã OTP xác thực - CKL Banking (Gửi lại)",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          status: "pending",
          isResend: true,
        });

    return {success: true, message: "OTP email will be resent"};
  } catch (error) {
    functions.logger.error("Error resending OTP:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// VNPay Helper Functions
// ============================================

/**
 * Generate VNPay payment URL
 * @param {Object} params - Payment parameters
 * @return {string} Payment URL
 */
function generateVNPayURL(params) {
  const {
    amount,
    orderId,
    orderDescription,
    orderType,
    locale = "vn",
    ipAddr,
  } = params;

  const date = new Date();
  const createDate = date.toISOString().replace(/[-:]/g, "").split(".")[0];
  const expireDate = new Date(date.getTime() + 15 * 60 * 1000)
      .toISOString()
      .replace(/[-:]/g, "")
      .split(".")[0];

  const vnpParams = {
    vnp_Version: "2.1.0",
    vnp_Command: "pay",
    vnp_TmnCode: VNPAY_CONFIG.tmnCode,
    vnp_Amount: Math.round(amount * 100), // Convert to cents
    vnp_CurrCode: "VND",
    vnp_TxnRef: orderId,
    vnp_OrderInfo: orderDescription,
    vnp_OrderType: orderType || "other",
    vnp_Locale: locale,
    vnp_ReturnUrl: VNPAY_CONFIG.returnUrl,
    vnp_IpAddr: ipAddr,
    vnp_CreateDate: createDate,
    vnp_ExpireDate: expireDate,
  };

  // Sort parameters
  const sortedParams = Object.keys(vnpParams)
      .sort()
      .reduce((result, key) => {
        result[key] = vnpParams[key];
        return result;
      }, {});

  // Create query string
  const queryString = Object.keys(sortedParams)
      .map((key) => `${key}=${encodeURIComponent(sortedParams[key])}`)
      .join("&");

  // Create secure hash
  const hmac = crypto.createHmac("sha512", VNPAY_CONFIG.secretKey);
  hmac.update(queryString);
  // eslint-disable-next-line camelcase
  const vnp_SecureHash = hmac.digest("hex");

  // Add secure hash to params
  // eslint-disable-next-line camelcase
  const finalQueryString = `${queryString}&vnp_SecureHash=${vnp_SecureHash}`;

  return `${VNPAY_CONFIG.url}?${finalQueryString}`;
}

/**
 * Verify VNPay callback
 * @param {Object} params - Callback parameters
 * @return {boolean} Verification result
 */
function verifyVNPayCallback(params) {
  // eslint-disable-next-line camelcase
  const secureHash = params.vnp_SecureHash;
  // eslint-disable-next-line camelcase
  delete params.vnp_SecureHash;
  // eslint-disable-next-line camelcase
  delete params.vnp_SecureHashType;

  // Sort parameters
  const sortedParams = Object.keys(params)
      .sort()
      .reduce((result, key) => {
        result[key] = params[key];
        return result;
      }, {});

  // Create query string
  const queryString = Object.keys(sortedParams)
      .map((key) => `${key}=${encodeURIComponent(sortedParams[key])}`)
      .join("&");

  // Create secure hash
  const hmac = crypto.createHmac("sha512", VNPAY_CONFIG.secretKey);
  hmac.update(queryString);
  const calculatedHash = hmac.digest("hex");

  return secureHash === calculatedHash;
}

// ============================================
// Cloud Function: Process VNPay Payment
// ============================================
exports.processVNPayPayment = functions.firestore
    .onDocumentCreated("payment_requests/{requestId}", async (event) => {
      const snap = event.data;
      const context = event.params;
      const paymentData = snap.data();

      // Only process VNPay payments
      if (paymentData.paymentMethod !== "vnpay") {
        functions.logger.log("Not a VNPay payment, skipping...");
        return null;
      }

      // Only process if status is pending
      if (paymentData.status !== "pending") {
        functions.logger.log("Payment already processed, skipping...");
        return null;
      }

      const transactionId = paymentData.transactionId;
      const amount = paymentData.amount;
      const recipientBank = paymentData.recipientBank || "Unknown";

      functions.logger.log("Processing VNPay payment:", {
        transactionId: transactionId,
        amount: amount,
        recipientBank: recipientBank,
      });

      try {
        // Update status to processing
        await snap.ref.update({
          status: "processing",
          processingAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Generate VNPay payment URL
        const paymentURL = generateVNPayURL({
          amount: amount,
          orderId: transactionId,
          orderDescription: `Chuyen tien den ${recipientBank}`,
          orderType: "other",
          locale: "vn",
          ipAddr: paymentData.clientIp || "127.0.0.1",
        });

        functions.logger.log("VNPay payment URL generated:", paymentURL);

        // Update payment request with payment URL
        await snap.ref.update({
          status: "pending_payment",
          paymentUrl: paymentURL,
          paymentGateway: "VNPay",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // In production, you might want to:
        // 1. Send payment URL to client via FCM notification
        // 2. Store payment URL for later verification
        // 3. Set up webhook to handle VNPay callback

        return null;
      } catch (error) {
        functions.logger.error("Error processing VNPay payment:", error);

        await snap.ref.update({
          status: "failed",
          error: error.message,
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        return null;
      }
    });

// ============================================
// Cloud Function: VNPay Callback Handler
// ============================================
exports.vnpayCallback = functions.https.onRequest(async (req, res) => {
  functions.logger.log("VNPay callback received:", req.query);

  const vnpParams = req.query;

  try {
    // Verify callback signature
    const isValid = verifyVNPayCallback({...vnpParams});

    if (!isValid) {
      functions.logger.error("Invalid VNPay callback signature");
      return res.status(400).json({
        RspCode: "97",
        Message: "Checksum failed",
      });
    }

    const orderId = vnpParams.vnp_TxnRef;
    const responseCode = vnpParams.vnp_ResponseCode;
    const transactionNo = vnpParams.vnp_TransactionNo;
    const amount = parseInt(vnpParams.vnp_Amount) / 100; // Convert from cents

    // Find payment request
    const paymentRequest = await admin.firestore()
        .collection("payment_requests")
        .doc(orderId)
        .get();

    if (!paymentRequest.exists) {
      functions.logger.error("Payment request not found:", orderId);
      return res.status(404).json({
        RspCode: "01",
        Message: "Order not found",
      });
    }

    // Check response code
    if (responseCode === "00") {
      // Payment successful
      await paymentRequest.ref.update({
        status: "completed",
        paymentReference: transactionNo,
        vnpResponseCode: responseCode,
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        vnpParams: vnpParams,
      });

      functions.logger.log("VNPay payment successful:", {
        orderId: orderId,
        transactionNo: transactionNo,
        amount: amount,
      });

      return res.status(200).json({
        RspCode: "00",
        Message: "Success",
      });
    } else {
      // Payment failed
      await paymentRequest.ref.update({
        status: "failed",
        vnpResponseCode: responseCode,
        error: vnpParams.vnp_ResponseCode,
        failedAt: admin.firestore.FieldValue.serverTimestamp(),
        vnpParams: vnpParams,
      });

      functions.logger.error("VNPay payment failed:", {
        orderId: orderId,
        responseCode: responseCode,
      });

      return res.status(200).json({
        RspCode: responseCode,
        Message: "Payment failed",
      });
    }
  } catch (error) {
    functions.logger.error("Error handling VNPay callback:", error);
    return res.status(500).json({
      RspCode: "99",
      Message: "Internal error",
    });
  }
});

// ============================================
// Cloud Function: Send Password Reset Email (Firestore Trigger)
// ============================================
exports.sendPasswordResetEmail = functions.firestore
    .onDocumentCreated("password_reset_requests/{requestId}", async (event) => {
      const snap = event.data;
      const requestId = event.params.requestId;
      
      if (!snap) {
        functions.logger.error("No snapshot data in password reset request");
        return null;
      }
      
      const requestData = snap.data();
      if (!requestData) {
        functions.logger.error("No data in password reset request snapshot");
        return null;
      }
      
      const email = requestData.email;

      if (!email) {
        functions.logger.error("Missing email in password reset request");
        try {
          await snap.ref.update({
            status: "failed",
            error: "Missing email",
            failedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        } catch (err) {
          functions.logger.error("Error updating failed status:", err);
        }
        return null;
      }

      // Only process if status is pending
      if (requestData.status !== "pending") {
        functions.logger.log("Password reset request already processed, status:", requestData.status);
        return null;
      }
      
      functions.logger.log("Processing password reset request for:", email);

      try {
        // Generate reset token
        const resetToken = crypto.randomBytes(32).toString("hex");
        // TODO: Update this URL to your actual app reset password page
        const resetLink = process.env.RESET_PASSWORD_URL ||
          `https://ckl-banking.web.app/reset-password?token=${resetToken}`;

        // Store reset token in Firestore (expires in 1 hour)
        await admin.firestore()
            .collection("password_resets")
            .doc(resetToken)
            .set({
              email: email,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              expiresAt: new Date(Date.now() + 60 * 60 * 1000), // 1 hour
              used: false,
            });

    // Email HTML template
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đặt lại mật khẩu - CKL Banking</title>
      </head>
      <body style="margin: 0; padding: 0; font-family: Arial, sans-serif;
        background-color: #f5f5f5;">
        <table role="presentation" style="width: 100%;
          border-collapse: collapse;">
          <tr>
            <td style="padding: 20px 0; text-align: center;
              background-color: #1976D2;">
              <h1 style="color: #ffffff; margin: 0; font-size: 24px;">
                CKL Banking
              </h1>
            </td>
          </tr>
          <tr>
            <td style="padding: 40px 20px; background-color: #ffffff;">
              <table role="presentation" style="width: 100%;
                max-width: 600px; margin: 0 auto;">
                <tr>
                  <td>
                    <h2 style="color: #333333; margin: 0 0 20px 0;">
                      Đặt lại mật khẩu
                    </h2>
                    <p style="color: #666666; font-size: 16px;
                      line-height: 1.5; margin: 0 0 20px 0;">
                      Xin chào,
                    </p>
                    <p style="color: #666666; font-size: 16px;
                      line-height: 1.5; margin: 0 0 30px 0;">
                      Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản
                      <strong>${email}</strong>.
                    </p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                      <a href="${resetLink}" style="background-color: #1976D2;
                        color: #ffffff; padding: 15px 30px;
                        text-decoration: none; border-radius: 5px;
                        display: inline-block; font-weight: bold;">
                        Đặt lại mật khẩu
                      </a>
                    </div>
                    
                    <p style="color: #666666; font-size: 14px;
                      line-height: 1.5; margin: 20px 0;">
                      Hoặc copy link sau vào trình duyệt:<br>
                      <a href="${resetLink}" style="color: #1976D2;
                        word-break: break-all;">${resetLink}</a>
                    </p>
                    
                    <p style="color: #ff6b6b; font-size: 14px;
                      font-weight: bold; margin: 20px 0;">
                      ⏰ Link này có hiệu lực trong <strong>1 giờ</strong>
                    </p>
                    
                    <div style="background-color: #fff3cd;
                      border-left: 4px solid #ffc107; padding: 15px;
                      margin: 30px 0; border-radius: 4px;">
                      <p style="color: #856404; margin: 0; font-size: 14px;">
                        <strong>⚠️ Lưu ý bảo mật:</strong><br>
                        • Nếu bạn không yêu cầu đặt lại mật khẩu,
                        vui lòng bỏ qua email này<br>
                        • Không chia sẻ link này với bất kỳ ai<br>
                        • CKL Banking sẽ không bao giờ yêu cầu bạn
                        cung cấp mật khẩu qua email
                      </p>
                    </div>
                    
                    <hr style="border: none; border-top: 1px solid #eeeeee;
                      margin: 30px 0;">
                    
                    <p style="color: #999999; font-size: 12px; margin: 0;
                      text-align: center;">
                      Đây là email tự động, vui lòng không trả lời email này.<br>
                      © ${new Date().getFullYear()} CKL Banking.
                      All rights reserved.
                    </p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>
      </body>
      </html>
    `;

    const mailOptions = {
      from: `CKL Banking <${GMAIL_USER}>`,
      to: email,
      subject: "Đặt lại mật khẩu - CKL Banking",
      html: htmlContent,
      text: `Bạn đã yêu cầu đặt lại mật khẩu. Truy cập link sau để đặt lại: ${resetLink}. Link này có hiệu lực trong 1 giờ.`,
    };

        // Send email
        await transporter.sendMail(mailOptions);
        functions.logger.log("Password reset email sent to:", email);

        // Update request status
        await snap.ref.update({
          status: "sent",
          resetToken: resetToken,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        return null;
      } catch (error) {
        functions.logger.error("Error sending password reset email:", error);

        // Update request status to failed
        await snap.ref.update({
          status: "failed",
          error: error.message,
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        return null;
      }
    });

// ============================================
// Cloud Function: Check VNPay Payment Status
// ============================================
exports.checkVNPayStatus = functions.https.onCall(async (data, context) => {
  // Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated",
    );
  }

  const transactionId = data.transactionId;

  if (!transactionId) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Transaction ID is required",
    );
  }

  try {
    const paymentRequest = await admin.firestore()
        .collection("payment_requests")
        .doc(transactionId)
        .get();

    if (!paymentRequest.exists) {
      throw new functions.https.HttpsError(
          "not-found",
          "Payment request not found",
      );
    }

    const paymentData = paymentRequest.data();

    return {
      success: true,
      status: paymentData.status,
      paymentUrl: paymentData.paymentUrl,
      paymentReference: paymentData.paymentReference,
      error: paymentData.error,
    };
  } catch (error) {
    functions.logger.error("Error checking VNPay status:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});
