// @unocss-include
import { getRgb } from '@sa/color';
import { localStg } from '@/utils/storage';
import { $t } from '@/locales';

export function setupLoading() {
  const themeColor = localStg.get('themeColor') || '#646cff';

  const { r, g, b } = getRgb(themeColor);

  const primaryColor = `--primary-color: ${r} ${g} ${b}`;
  const cssVars = `${primaryColor};`;

  const loadingClasses = [
    'left-0 top-0',
    'left-0 bottom-0 animate-delay-500',
    'right-0 top-0 animate-delay-1000',
    'right-0 bottom-0 animate-delay-1500'
  ];

  const dot = loadingClasses
    .map(item => {
      return `<div class="absolute w-16px h-16px bg-primary rounded-8px animate-pulse ${item}"></div>`;
    })
    .join('\n');

  const loading = `
<div class="fixed-center flex-col bg-layout" style="${cssVars}">
  <div class="w-128px h-128px">
    ${getLogoSvg()}
  </div>
  <div class="w-56px h-56px my-36px">
    <div class="relative h-full animate-spin">
      ${dot}
    </div>
  </div>
  <h2 class="text-28px font-500 text-primary">${$t('system.title')}</h2>
</div>`;

  const app = document.getElementById('app');

  if (app) {
    app.innerHTML = loading;
  }
}

function getLogoSvg() {
  const logoSvg = `<svg
        width="100%"
        height="100%"
        viewBox="0 0 120 64"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-label="Jasic logo"
      >
        <defs>
          <linearGradient id="jasicTop" x1="9" y1="6" x2="45" y2="27" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#ffd98c" />
            <stop offset="0.52" stop-color="#f5a934" />
            <stop offset="1" stop-color="#cf7109" />
          </linearGradient>
          <linearGradient id="jasicBottom" x1="18" y1="25" x2="110" y2="57" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#7a7a7a" />
            <stop offset="0.48" stop-color="#3f3f3f" />
            <stop offset="1" stop-color="#111111" />
          </linearGradient>
          <linearGradient id="jasicEdge" x1="9" y1="26" x2="52" y2="34" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#f4f4f4" />
            <stop offset="1" stop-color="#d9d9d9" />
          </linearGradient>
        </defs>
        <path d="M8 28 L29 5 L55 18 L31 32 Z" fill="url(#jasicTop)" />
        <path d="M10 31 C27 28 47 26 66 27 C87 28 104 33 114 41 C118 44 120 47 120 49 C120 55 112 59 95 60 L35 60 C20 60 12 57 9 50 C8 46 8 39 10 31 Z" fill="url(#jasicBottom)" />
        <path d="M13 34 L31 35 L42 32 L31 39 L11 37 Z" fill="url(#jasicEdge)" opacity="0.95" />
      </svg>
  `;

  return logoSvg;
}
