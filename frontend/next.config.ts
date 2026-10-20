import type { NextConfig } from 'next';
import { validateEnv } from './src/lib/env';

validateEnv();

const nextConfig: NextConfig = {};

export default nextConfig;
