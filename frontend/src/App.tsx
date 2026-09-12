import { DeviceDetail } from "./components/DeviceDetail";
import { DeviceList } from "./components/DeviceList";
import { EmptyState } from "./components/EmptyState";
import { Header } from "./components/Header";
import { useDashboard } from "./hooks/useDashboard";

export function App() {
  const dashboard = useDashboard();

  return (
    <div className="app-shell">
      <Header health={dashboard.health} live={dashboard.live} deviceCount={dashboard.devices.length} />
      {dashboard.error && <div className="banner">{dashboard.error}</div>}
      <main className="workspace">
        <DeviceList
          devices={dashboard.devices}
          selectedId={dashboard.selectedId}
          onSelect={dashboard.setSelectedId}
        />
        {dashboard.selected ? (
          <DeviceDetail
            device={dashboard.selected}
            history={dashboard.history}
            ledBusy={dashboard.ledBusy}
            onToggleLed={() => void dashboard.toggleLed()}
          />
        ) : (
          <EmptyState
            backendDown={!dashboard.loading && dashboard.health == null}
            onRetry={() => void dashboard.refresh()}
          />
        )}
      </main>
    </div>
  );
}
