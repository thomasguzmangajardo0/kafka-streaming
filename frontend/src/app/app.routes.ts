import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login').then((m) => m.Login) },
  { path: 'pedidos', canActivate: [authGuard], loadComponent: () => import('./pages/pedidos').then((m) => m.Pedidos) },
  { path: 'inventario', canActivate: [authGuard], loadComponent: () => import('./pages/inventario').then((m) => m.Inventario) },
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notificaciones').then((m) => m.Notificaciones),
  },
  { path: '', pathMatch: 'full', redirectTo: 'pedidos' },
  { path: '**', redirectTo: 'pedidos' },
];
