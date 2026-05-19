// Catches React rendering errors that would otherwise white-screen the app.
// On error: shows a calm fallback with the shop's phone number so a customer
// can still reach the business even if something software-side broke.

import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // Log to console; in production we could ship this to a logging endpoint.
    console.error('[ErrorBoundary]', error, info?.componentStack);
  }

  reset = () => {
    this.setState({ error: null });
    window.location.assign('/');
  };

  render() {
    if (!this.state.error) return this.props.children;

    return (
      <div style={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        background: '#FFF8F0',
        color: '#1A1209',
        fontFamily: '"Plus Jakarta Sans", system-ui, sans-serif',
      }}>
        <div style={{ maxWidth: 360, textAlign: 'center' }}>
          <div style={{
            width: 64, height: 64, borderRadius: 16,
            background: '#DC4F00', color: 'white',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 900, fontSize: 28, margin: '0 auto 20px',
            letterSpacing: '-2px',
          }}>SK</div>

          <h1 style={{ fontSize: 22, fontWeight: 800, margin: 0 }}>
            Something went wrong
          </h1>
          <p style={{ color: '#5C5247', marginTop: 8, lineHeight: 1.5 }}>
            We hit a small problem. Please refresh or come back in a minute.
            Need help right now? Call the shop directly.
          </p>

          <a
            href="tel:+918960245022"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              marginTop: 20, padding: '12px 18px',
              background: '#047857', color: 'white',
              borderRadius: 14, fontWeight: 700, textDecoration: 'none',
            }}
          >
            📞 Call +91 89602 45022
          </a>

          <button
            onClick={this.reset}
            style={{
              display: 'block', margin: '12px auto 0',
              background: 'transparent', border: '1px solid #DDD2BE',
              color: '#1A1209', padding: '10px 16px', borderRadius: 12,
              fontWeight: 600, cursor: 'pointer',
            }}
          >
            Reload the app
          </button>

          <details style={{ marginTop: 18, textAlign: 'left', fontSize: 11, color: '#8B8377' }}>
            <summary style={{ cursor: 'pointer' }}>Technical details</summary>
            <pre style={{ marginTop: 8, padding: 10, background: '#F5F0E8', borderRadius: 8, overflow: 'auto' }}>
              {String(this.state.error?.message || this.state.error)}
            </pre>
          </details>
        </div>
      </div>
    );
  }
}
