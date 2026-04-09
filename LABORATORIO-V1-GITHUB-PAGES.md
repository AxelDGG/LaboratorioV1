# Laboratorio 1

## Despliegue básico con GitHub Pages, pipeline y promoción de `develop` a `staging`

---

## Propósito del laboratorio

En este laboratorio construirás una aplicación web mínima y la llevarás desde tu entorno local hasta un entorno publicado en GitHub Pages, usando un pipeline de GitHub Actions.

El objetivo es que practiques de forma concreta:

- despliegue de una aplicación estática
- validación automática mediante pipeline
- promoción de cambios entre ramas
- publicación controlada de un entorno `staging`

---

## Resultados de aprendizaje

Al finalizar este laboratorio, deberías poder:

- diferenciar el papel de `local`, `develop` y `staging`
- explicar qué valida un pipeline antes de desplegar
- configurar un workflow básico de GitHub Actions
- publicar una aplicación React en GitHub Pages
- promover cambios desde `develop` hasta `staging`

---

## Escenario de trabajo

Trabajarás con una aplicación React creada con Vite.

El flujo será el siguiente:

1. desarrollar localmente
2. subir cambios a GitHub
3. ejecutar validaciones automáticas
4. generar un artefacto para `develop`
5. promover los cambios a `staging`
6. publicar `staging` en GitHub Pages

---

## Requisitos

Necesitas:

- Git
- Node.js 20 o superior
- una cuenta de GitHub
- un repositorio público en GitHub

---

## Arquitectura mínima del laboratorio

```text
Tu laptop
  -> git push
GitHub
  -> GitHub Actions
  -> quality checks
  -> artefacto de develop
  -> despliegue de staging
GitHub Pages
  -> sitio publicado
```

---

## Estructura esperada del proyecto

```text
deploy-v1-pages/
├─ .github/
│  └─ workflows/
│     └─ deploy-pages.yml
├─ src/
│  ├─ App.tsx
│  ├─ main.tsx
│  └─ styles.css
├─ index.html
├─ package.json
├─ tsconfig.json
├─ vite.config.ts
└─ public/
```

---

## Parte A. Construcción de la aplicación

### Paso 1. Crear la app con Vite

En tu terminal:

```powershell
npm create vite@latest deploy-v1-pages -- --template react-ts
cd deploy-v1-pages
npm install
```

Si usas Linux o macOS:

```bash
npm create vite@latest deploy-v1-pages -- --template react-ts
cd deploy-v1-pages
npm install
```

---

### Paso 2. Reemplazar `src/App.tsx`

Usa este contenido:

```tsx
import "./styles.css";

const environment = import.meta.env.VITE_PUBLIC_ENVIRONMENT || "local";
const version = import.meta.env.VITE_PUBLIC_VERSION || "dev-local";

const notes = [
  "Pipeline de calidad activo",
  "Promoción controlada de develop a staging",
  "Despliegue de staging en GitHub Pages"
];

export default function App() {
  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Laboratorio 1 · Despliegue con GitHub</p>
        <h1>Release Board V1</h1>
        <p className="hero-copy">
          Aplicación mínima para practicar pipeline, despliegue y promoción entre entornos.
        </p>
      </section>

      <section className="grid">
        <article className="card card-accent">
          <h2>Entorno actual</h2>
          <p className="badge">{environment}</p>
          <p>Este valor cambia en cada build y nos ayuda a verificar qué entorno estamos viendo.</p>
        </article>

        <article className="card">
          <h2>Versión visible</h2>
          <p className="mono">{version}</p>
          <p>Usaremos el SHA corto del commit para identificar qué versión llegó a staging.</p>
        </article>

        <article className="card">
          <h2>Qué estamos practicando</h2>
          <ul>
            {notes.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>
      </section>
    </main>
  );
}
```

---

### Paso 3. Reemplazar `src/styles.css`

Usa este contenido:

```css
:root {
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  color: #e8eef7;
  background:
    radial-gradient(circle at top, #244b73 0%, #0f172a 45%, #07111f 100%);
  line-height: 1.5;
  font-weight: 400;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}

#root {
  min-height: 100vh;
}

.shell {
  max-width: 1100px;
  margin: 0 auto;
  padding: 48px 20px 64px;
}

.hero {
  margin-bottom: 24px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.8rem;
  color: #93c5fd;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  font-size: clamp(2.2rem, 5vw, 4rem);
  margin-bottom: 12px;
}

.hero-copy {
  max-width: 720px;
  color: #cbd5e1;
  font-size: 1.05rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.card {
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 20px;
  backdrop-filter: blur(10px);
}

.card-accent {
  border-color: rgba(96, 165, 250, 0.45);
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: #1d4ed8;
  font-weight: 700;
  text-transform: uppercase;
}

.mono {
  font-family: "Consolas", "Courier New", monospace;
  color: #fde68a;
}

ul {
  padding-left: 18px;
  margin-bottom: 0;
}
```

---

### Paso 4. Ajustar `vite.config.ts`

Reemplaza el contenido por:

```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";

export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE_PATH || "/"
});
```

---

### Paso 5. Ejecutar la app localmente

Ejecuta:

```powershell
npm run dev
```

Abre:

[http://localhost:5173](http://localhost:5173)

Verifica que aparezcan:

- el título `Release Board V1`
- el entorno `local`
- la versión `dev-local`

---

## Parte B. Preparación del repositorio

### Paso 6. Inicializar Git

En la raíz del proyecto:

```powershell
git init
git add .
git commit -m "feat: initial laboratory app"
```

---

### Paso 7. Crear el repositorio en GitHub

1. Crea un repositorio público.
2. Conéctalo desde tu terminal.

Ejemplo:

```powershell
git remote add origin TU_URL_DEL_REPOSITORIO
git branch -M main
git push -u origin main
```

---

### Paso 8. Crear ramas de trabajo

Esta práctica usa:

- `develop`
- `staging`

Créelas así:

```powershell
git checkout -b develop
git push -u origin develop
git checkout -b staging
git push -u origin staging
git checkout develop
```

---

## Parte C. Activación del despliegue

### Paso 9. Activar GitHub Pages

En tu repositorio entra a:

`Settings -> Pages`

En `Build and deployment` configura:

- Source: `GitHub Actions`

---

### Paso 10. Crear el workflow

Crea `.github/workflows/deploy-pages.yml` con este contenido:

```yaml
name: deploy-pages

on:
  pull_request:
    branches: [develop, staging]
  push:
    branches: [develop, staging]

permissions:
  contents: read

concurrency:
  group: "pages"
  cancel-in-progress: true

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build develop preview
        if: github.ref == 'refs/heads/develop' || github.base_ref == 'develop'
        run: npm run build
        env:
          VITE_PUBLIC_ENVIRONMENT: dev
          VITE_PUBLIC_VERSION: ${{ github.sha }}
          VITE_BASE_PATH: /${{ github.event.repository.name }}/

      - name: Build staging site
        if: github.ref == 'refs/heads/staging' || github.base_ref == 'staging'
        run: npm run build
        env:
          VITE_PUBLIC_ENVIRONMENT: staging
          VITE_PUBLIC_VERSION: ${{ github.sha }}
          VITE_BASE_PATH: /${{ github.event.repository.name }}/

      - name: Upload preview artifact for develop
        if: github.ref == 'refs/heads/develop' && github.event_name == 'push'
        uses: actions/upload-artifact@v4
        with:
          name: develop-dist
          path: dist

  deploy-staging:
    if: github.ref == 'refs/heads/staging' && github.event_name == 'push'
    needs: quality
    permissions:
      contents: read
      pages: write
      id-token: write
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build staging
        run: npm run build
        env:
          VITE_PUBLIC_ENVIRONMENT: staging
          VITE_PUBLIC_VERSION: ${{ github.sha }}
          VITE_BASE_PATH: /${{ github.event.repository.name }}/

      - name: Setup Pages
        uses: actions/configure-pages@v5

      - name: Upload Pages artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: ./dist

      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

---

### Paso 11. Subir el workflow a `develop`

```powershell
git checkout develop
git add .
git commit -m "feat: add github pages workflow"
git push origin develop
```

---

### Paso 12. Verificar el pipeline de `develop`

En la pestaña `Actions`, verifica que:

- el job `quality` finaliza correctamente
- se genera el artefacto `develop-dist`

En esta etapa no hay URL pública todavía.

---

## Parte D. Promoción entre entornos

### Paso 13. Hacer un cambio visible en `develop`

Edita `src/App.tsx` y agrega un nuevo punto a la lista:

```tsx
const notes = [
  "Pipeline de calidad activo",
  "Promoción controlada de develop a staging",
  "Despliegue de staging en GitHub Pages",
  "Cambio visible desde develop"
];
```

Sube el cambio:

```powershell
git add .
git commit -m "feat: add visible change in develop"
git push origin develop
```

Verifica nuevamente `Actions`.

---

### Paso 14. Promover a `staging`

```powershell
git checkout staging
git pull origin staging
git merge develop
git push origin staging
```

---

### Paso 15. Verificar el despliegue de `staging`

En GitHub Actions verifica que:

- `quality` termina correctamente
- `deploy-staging` termina correctamente

Luego abre la URL publicada en:

`Settings -> Pages`

Debes observar:

- entorno `staging`
- versión basada en el SHA del commit
- el cambio visible que promoviste desde `develop`

---

## Entregables

Debes entregar lo siguiente:

### Entregable 1. URL del repositorio

Comparte la URL pública de tu repositorio.

### Entregable 2. URL de GitHub Pages

Comparte la URL pública de tu entorno `staging`.

### Entregable 3. Evidencia de `develop`

Incluye una captura donde se vea:

- el workflow ejecutado sobre `develop`
- el job `quality`
- el artefacto `develop-dist`

### Entregable 4. Evidencia de promoción

Incluye una captura o breve explicación donde se vea:

- el merge de `develop` hacia `staging`
- el workflow exitoso de `staging`

### Entregable 5. Evidencia del sitio publicado

Incluye una captura del sitio desplegado en GitHub Pages donde se observe:

- `staging` como entorno actual
- la versión visible

### Entregable 6. Reflexión breve

Responde en 5 a 8 líneas:

- qué diferencia encontraste entre `develop` y `staging`
- qué papel cumple GitHub Actions
- por qué no basta con que la app funcione en local

---

## Criterios de evaluación

### Criterio 1. Construcción local

Se espera que:

- la aplicación ejecute en local
- la interfaz muestre entorno y versión

Valor sugerido: 20%

### Criterio 2. Estructura del repositorio

Se espera que:

- exista el proyecto en GitHub
- existan las ramas `develop` y `staging`
- el código esté versionado con orden mínimo

Valor sugerido: 15%

### Criterio 3. Pipeline funcional

Se espera que:

- el workflow de GitHub Actions exista
- el job `quality` corra correctamente
- `develop` genere el artefacto esperado

Valor sugerido: 25%

### Criterio 4. Promoción entre entornos

Se espera que:

- el cambio se trabaje primero en `develop`
- luego se promueva a `staging`
- el cambio aparezca en el sitio publicado

Valor sugerido: 25%

### Criterio 5. Comprensión conceptual

Se espera que el estudiante pueda explicar:

- la diferencia entre validar y desplegar
- el rol de `develop` y `staging`
- el valor de la promoción controlada

Valor sugerido: 15%

---

## Lista de verificación final

- [ ] la app corre en local
- [ ] `vite.config.ts` está configurado para GitHub Pages
- [ ] el repositorio es accesible
- [ ] existen `develop` y `staging`
- [ ] `develop` genera el artefacto `develop-dist`
- [ ] `staging` publica en GitHub Pages
- [ ] el cambio visible fue promovido correctamente
- [ ] la reflexión final fue entregada

---

## Errores comunes

### La página publicada carga en blanco

Revisa:

- la configuración `base` en `vite.config.ts`
- la variable `VITE_BASE_PATH` en el workflow

### GitHub Pages no publica nada

Revisa:

- `Settings -> Pages`
- `Source: GitHub Actions`

### `develop` no tiene URL pública

En este laboratorio, eso es correcto.

`develop` sirve para integrar y validar.
`staging` es el entorno publicado.

### `staging` no refleja cambios

Revisa:

- que el cambio sí esté en `develop`
- que hayas hecho merge de `develop` hacia `staging`
- que el workflow de `staging` haya terminado bien

---

## Cierre

Al completar este laboratorio ya habrás practicado una ruta básica, pero real, de entrega:

- desarrollo local
- versionado
- validación automática
- promoción entre ramas
- despliegue controlado

Esa base será suficiente para avanzar después hacia prácticas más completas con backend, base de datos, secretos y servicios externos.

