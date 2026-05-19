import { useNavigate } from 'react-router-dom';
import { ShieldOff, ArrowLeft, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';

export default function Unauthorized() {
  const navigate = useNavigate();
  const { isAuthenticated, logout } = useAuth();

  return (
    <div className="min-h-dvh bg-canvas flex items-center justify-center px-6">
      <div className="text-center max-w-xs animate-fade-up">
        <div className="w-16 h-16 bg-urgent-tint text-urgent rounded-2xl flex items-center justify-center mx-auto mb-5">
          <ShieldOff size={28} strokeWidth={1.8} />
        </div>
        <h1 className="text-2xl font-display font-bold text-ink">Access denied</h1>
        <p className="text-sm text-ink-3 mt-2">
          You don't have permission to view this page. If this is unexpected, please contact the shop owner.
        </p>

        <div className="flex flex-col gap-2 mt-6">
          <Button variant="outline" size="lg" fullWidth icon={ArrowLeft} onClick={() => navigate(-1)}>
            Go back
          </Button>
          {isAuthenticated && (
            <Button variant="ghost" size="md" fullWidth icon={LogOut} onClick={logout}>
              Sign out
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
