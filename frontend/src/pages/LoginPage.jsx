// LoginPage — SK Electronics, Banda
// Light, warm, trustworthy. Looks like a real shop's tool, not a SaaS startup.

import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/auth';
import toast from 'react-hot-toast';
import { ArrowRight, RotateCcw, ShieldCheck, MapPin, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/Button';

const STEP = { PHONE: 'phone', OTP: 'otp', LOADING: 'loading' };

// ─── Brand mark — a clean, custom monogram (no generic Zap) ────────────────
function ShopMark({ size = 56 }) {
  return (
    <div
      className="flex items-center justify-center rounded-2xl bg-brand text-white font-display font-extrabold shadow-brand"
      style={{ width: size, height: size, fontSize: size * 0.42, letterSpacing: '-0.06em' }}
      aria-label="SK Electronics"
    >
      SK
    </div>
  );
}

export default function LoginPage() {
  const { login, isAuthenticated, role } = useAuth();
  const navigate = useNavigate();
  const [step, setStep]   = useState(STEP.PHONE);
  const [phone, setPhone] = useState('');
  const [otp, setOtp]     = useState(['', '', '', '', '', '']);
  const [busy, setBusy]   = useState(false);
  const otpRefs = useRef([]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate(role === 'ADMIN' ? '/admin' : '/tech', { replace: true });
    }
  }, [isAuthenticated, role, navigate]);

  const sendOtp = async (e) => {
    e.preventDefault();
    if (!/^[6-9]\d{9}$/.test(phone)) { toast.error('Please enter a valid 10-digit mobile number'); return; }
    setBusy(true);
    try {
      await authApi.sendOtp(phone);
      setStep(STEP.OTP);
      toast.success('OTP sent to your phone');
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not send OTP. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const handleOtpKey = (i, e) => {
    if (e.key === 'Backspace') {
      const updated = [...otp];
      if (otp[i]) { updated[i] = ''; setOtp(updated); }
      else if (i > 0) { updated[i-1] = ''; setOtp(updated); otpRefs.current[i-1]?.focus(); }
    }
  };

  const handleOtpInput = (i, val) => {
    if (!/^\d?$/.test(val)) return;
    const updated = [...otp];
    updated[i] = val.slice(-1);
    setOtp(updated);
    if (val && i < 5) otpRefs.current[i+1]?.focus();
  };

  const handleOtpPaste = (e) => {
    const paste = e.clipboardData.getData('text').replace(/\D/g,'').slice(0,6);
    if (paste.length === 6) {
      setOtp(paste.split(''));
      otpRefs.current[5]?.focus();
    }
  };

  const verifyOtp = async (e) => {
    e.preventDefault();
    const code = otp.join('');
    if (code.length < 6) { toast.error('Please enter all 6 digits'); return; }
    setBusy(true);
    setStep(STEP.LOADING);
    try {
      const { data: res } = await authApi.verifyOtp(phone, code);
      const d = res.data;
      login(
        { userId: d.userId, phone, role: d.role, name: d.name || phone },
        d.accessToken,
        d.refreshToken,
      );
      toast.success(`Welcome back${d.name ? ', ' + d.name.split(' ')[0] : ''}!`);
      navigate(d.role === 'ADMIN' ? '/admin' : '/tech', { replace: true });
    } catch (err) {
      toast.error(err.response?.data?.message || 'Wrong OTP. Please try again.');
      setStep(STEP.OTP);
      setOtp(['', '', '', '', '', '']);
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-dvh bg-canvas flex flex-col items-center px-5 pt-10 pb-8">
      <div className="w-full max-w-sm animate-fade-up">

        {/* ── Brand block ────────────────────────────────────────────── */}
        <div className="flex flex-col items-center text-center mb-7">
          <ShopMark size={64} />
          <h1 className="mt-4 text-2xl font-display font-extrabold text-ink tracking-tight">
            SK Electronics
          </h1>
          <p className="hi text-sm text-brand mt-1">एसके इलेक्ट्रॉनिक्स</p>
          <p className="text-xs text-ink-3 mt-2 flex items-center gap-1.5">
            <MapPin size={12} />
            Banda, Uttar Pradesh
            <span className="text-line-2">·</span>
            <span>Since 2010</span>
          </p>
        </div>

        {/* ── Card ───────────────────────────────────────────────────── */}
        <div className="bg-paper rounded-2xl border border-line shadow-card p-6 sm:p-7">
          {step === STEP.LOADING ? (
            <div className="flex flex-col items-center gap-4 py-10">
              <Loader2 size={28} className="text-brand animate-spin" />
              <p className="text-sm text-ink-2">Signing you in…</p>
            </div>
          ) : step === STEP.PHONE ? (
            <form onSubmit={sendOtp} className="space-y-5">
              <div>
                <p className="hi text-sm text-ink-3 mb-1">साइन इन</p>
                <h2 className="text-xl font-bold text-ink">Sign in to your account</h2>
                <p className="text-sm text-ink-3 mt-1.5 leading-relaxed">
                  We'll send a 6-digit code to your phone.
                </p>
              </div>

              {/* Phone input — large, tap-friendly, +91 prefix on the same surface */}
              <div>
                <label className="block text-xs font-semibold text-ink-2 mb-1.5">
                  Mobile number <span className="text-urgent">*</span>
                </label>
                <div className="flex items-stretch bg-paper border border-line-2 rounded-xl overflow-hidden focus-within:border-brand focus-within:shadow-focus transition-all">
                  <span className="px-3.5 flex items-center bg-paper-3 text-ink-2 text-base font-mono border-r border-line-2">+91</span>
                  <input
                    type="tel"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={10}
                    value={phone}
                    onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
                    placeholder="98765 43210"
                    className="flex-1 bg-transparent px-3.5 py-3.5 text-base font-mono text-ink placeholder:text-ink-4 outline-none num"
                    autoFocus
                  />
                </div>
              </div>

              <Button
                type="submit"
                size="lg"
                fullWidth
                loading={busy}
                disabled={phone.length < 10}
                iconRight={ArrowRight}
              >
                Send OTP
              </Button>

              <div className="flex items-center gap-2 pt-2 text-xs text-ink-3">
                <ShieldCheck size={14} className="text-money" />
                <span>Your number stays private. We never call without your request.</span>
              </div>
            </form>
          ) : (
            <form onSubmit={verifyOtp} className="space-y-5">
              <div>
                <p className="hi text-sm text-ink-3 mb-1">OTP डालें</p>
                <h2 className="text-xl font-bold text-ink">Enter the 6-digit code</h2>
                <p className="text-sm text-ink-3 mt-1.5">
                  Sent to <span className="font-mono text-ink num">+91 {phone.slice(0, 5)} {phone.slice(5)}</span>
                </p>
              </div>

              <div onPaste={handleOtpPaste} className="flex gap-2 justify-between">
                {otp.map((digit, i) => (
                  <input
                    key={i}
                    ref={(el) => (otpRefs.current[i] = el)}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleOtpInput(i, e.target.value)}
                    onKeyDown={(e) => handleOtpKey(i, e)}
                    className={[
                      'w-12 h-14 text-center text-xl font-mono font-bold rounded-xl outline-none transition-all num',
                      'border-2 bg-paper',
                      digit
                        ? 'border-brand text-ink shadow-focus'
                        : 'border-line-2 text-ink-2 hover:border-line',
                    ].join(' ')}
                  />
                ))}
              </div>

              <Button
                type="submit"
                size="lg"
                fullWidth
                loading={busy}
                disabled={otp.join('').length < 6}
                iconRight={ArrowRight}
              >
                Verify &amp; sign in
              </Button>

              <button
                type="button"
                onClick={() => { setStep(STEP.PHONE); setOtp(['','','','','','']); }}
                className="w-full text-ink-3 text-sm flex items-center justify-center gap-1.5 hover:text-ink transition-colors py-1"
              >
                <RotateCcw size={13} /> Use a different number
              </button>
            </form>
          )}
        </div>

        {/* ── Footer trust strip ─────────────────────────────────────── */}
        <div className="mt-6 grid grid-cols-3 gap-2 text-center text-[11px] text-ink-3">
          <div className="bg-paper border border-line rounded-xl py-2.5 px-2">
            <p className="font-bold text-ink text-sm num">15+</p>
            <p className="leading-tight mt-0.5">Years</p>
          </div>
          <div className="bg-paper border border-line rounded-xl py-2.5 px-2">
            <p className="font-bold text-ink text-sm num">200+</p>
            <p className="leading-tight mt-0.5">Technicians</p>
          </div>
          <div className="bg-paper border border-line rounded-xl py-2.5 px-2">
            <p className="font-bold text-ink text-sm num">8000+</p>
            <p className="leading-tight mt-0.5">Customers</p>
          </div>
        </div>

        <p className="text-center text-[11px] text-ink-3 mt-6">
          SK Electronics · Banda · Reg. 2010
        </p>
      </div>
    </div>
  );
}
