---
name: Luminous Velocity Updated
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1b1b1b'
  on-surface-variant: '#424654'
  inverse-surface: '#303030'
  inverse-on-surface: '#f1f1f1'
  outline: '#737786'
  outline-variant: '#c3c6d7'
  surface-tint: '#0055d2'
  primary: '#0045ae'
  on-primary: '#ffffff'
  primary-container: '#105cdb'
  on-primary-container: '#dae2ff'
  inverse-primary: '#b2c5ff'
  secondary: '#4e6072'
  on-secondary: '#ffffff'
  secondary-container: '#cfe2f7'
  on-secondary-container: '#526477'
  tertiary: '#9e0514'
  on-tertiary: '#ffffff'
  tertiary-container: '#c12729'
  on-tertiary-container: '#ffdbd7'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#b2c5ff'
  on-primary-fixed: '#001848'
  on-primary-fixed-variant: '#0040a1'
  secondary-fixed: '#d2e4fa'
  secondary-fixed-dim: '#b6c8de'
  on-secondary-fixed: '#091d2d'
  on-secondary-fixed-variant: '#37485a'
  tertiary-fixed: '#ffdad6'
  tertiary-fixed-dim: '#ffb3ad'
  on-tertiary-fixed: '#410003'
  on-tertiary-fixed-variant: '#930010'
  background: '#f9f9f9'
  on-background: '#1b1b1b'
  surface-variant: '#e2e2e2'
typography:
  display-3l:
    fontFamily: Poppins
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 110%
  display-2l:
    fontFamily: Poppins
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 120%
  headline-xl:
    fontFamily: Poppins
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 120%
  headline-lg:
    fontFamily: Poppins
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 140%
  body-base:
    fontFamily: Poppins
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 140%
  body-sm:
    fontFamily: Poppins
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 140%
  label-xs:
    fontFamily: Poppins
    fontSize: 12px
    fontWeight: '500'
    lineHeight: auto
  label-2xs:
    fontFamily: Poppins
    fontSize: 10px
    fontWeight: '500'
    lineHeight: auto
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
  container-max: 1280px
---

## Brand & Style

The design system embodies a clean, modern, and energetic personality. It is designed to evoke a sense of efficiency and clarity through a bright, high-contrast aesthetic. The visual narrative leverages **Modern Minimalism** mixed with **Soft UI** elements, prioritizing generous whitespace to let content breathe while maintaining a professional and accessible tone. 

The user experience is defined by smooth transitions and a clear hierarchy, making it ideal for productivity tools or high-energy SaaS platforms where velocity and clarity are paramount.

## Colors

The palette is anchored by a vibrant range of "Brand Blues," transitioning from a deep, authoritative primary (#105CDB) to a bright, energetic action blue (#1A81F4). 

- **Primary:** Used for main actions, active states, and brand highlights.
- **Secondary:** A soft, desaturated blue used for subtle backgrounds, hover states on light elements, and secondary button fills.
- **Destructive:** A bold red (#CB2F2F) reserved strictly for critical warnings and permanent delete actions.
- **Neutrals:** Pure black is used for primary headings to ensure maximum legibility, while a scale of grays handles secondary text and disabled states. The background should predominantly utilize the ultra-light blue-tinted white (#F4F9FF) to maintain the "Luminous" theme.

## Typography

This design system utilizes **Poppins** across all layers to maintain a geometric yet friendly appearance. 

- **Headlines:** Use Bold (700) for large display text and SemiBold (600) for section headers to establish a strong vertical rhythm.
- **Body:** Standardized at 16px for optimal readability with a comfortable 140% line height.
- **Labels:** Smaller weights utilize Medium (500) to ensure legibility even at 10px and 12px sizes. 

On mobile devices, `display-3l` should be capped at 32px to avoid excessive line breaking, while `body-base` remains consistent for accessibility.

## Layout & Spacing

The layout philosophy follows a **Fluid Grid** model with a 12-column structure for desktop and a 4-column structure for mobile. 

- **Rhythm:** A 4px baseline grid governs all spacing. Vertical increments should primarily use 8px, 16px, 24px, and 32px to maintain mathematical harmony.
- **Whitespace:** Emphasize "Generous Whitespace." Component containers should use a minimum of 24px internal padding.
- **Breakpoints:** 
  - Mobile: 0 - 599px
  - Tablet: 600px - 1023px
  - Desktop: 1024px+

## Elevation & Depth

Hierarchy is established through **Tonal Layering** and **Ambient Shadows**. 

- **Surface Layers:** The base background is the tinted white (#F4F9FF). Cards and interactive containers sit on top of this using pure white (#FFFFFF).
- **Shadows:** Use extremely soft, high-diffusion shadows with a hint of blue in the tint (e.g., `0px 8px 24px rgba(16, 92, 219, 0.08)`). This avoids the "muddy" look of pure black shadows and reinforces the luminous brand identity.
- **Interactive Depth:** On hover, cards should slightly increase their shadow spread rather than moving, creating a subtle "lift" effect.

## Shapes

The shape language is defined by significant roundedness to feel approachable and modern. 

- **Base Radius:** 8px (0.5rem) for small components like checkboxes and tags.
- **Standard Radius:** 16px (1rem) for buttons and input fields.
- **Large Radius:** 24px (1.5rem) for cards and modal containers to match the `rounded-2xl` aesthetic seen in the visual references.
- **Pill:** Fully rounded (999px) is reserved for status badges and specific floating action buttons.

## Components

### Buttons
- **Primary:** Solid #105CDB fill with white text. 16px corner radius.
- **Secondary:** Light blue fill (#D6E9FF) with primary blue text (#105CDB). No border.
- **Destructive:** Solid #CB2F2F fill with white text.
- **Sizing:** Large (56px height), Medium (48px height), Small (36px height).

### Cards
- **Style:** Pure white background, 24px border radius, soft ambient shadow.
- **Padding:** 24px or 32px internal padding depending on content density.

### Navigation Bars
- **Mobile:** Bottom-fixed navigation with a blurred backdrop or solid white fill. Icons should use the primary blue for the active state and a muted gray for inactive.
- **Desktop:** Side-rail or top-header with a clean divider (#D6E9FF) rather than a heavy shadow.

### Input Fields
- **Style:** 16px radius, light gray border (#C6C2C2). On focus, the border transitions to Primary Blue (#105CDB) with a subtle outer glow.

### Interactive Elements
- **Checkboxes/Radios:** Use the 8px radius for checkboxes; Primary Blue fill when checked.
- **Progress Circles:** Use a thick stroke with the brand gradient (#105CDB to #1A81F4) for a dynamic feel.