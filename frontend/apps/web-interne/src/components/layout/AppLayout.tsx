import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { ChatbotWidget } from './ChatbotWidget'

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="min-h-screen bg-surface">
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="lg:pl-[272px]">
        <Topbar onMenu={() => setMenuOpen(true)} />
        <main className="mx-auto w-full max-w-[1560px] px-4 py-5 sm:px-6 lg:px-7 lg:py-7">
          <Outlet />
        </main>
      </div>
      <ChatbotWidget />
    </div>
  )
}
