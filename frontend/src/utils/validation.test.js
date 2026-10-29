import { describe, expect, it } from 'vitest'
import {
  isRequired,
  isValidEmail,
  isValidBpm,
  isValidHttpsUrl,
  isValidPassword,
  isValidCardNumber,
  isValidExpiry,
  isValidCvv,
  maskCard,
} from './validation'

const today = new Date(2026, 9, 29) // 29/10/2026

describe('validation', () => {
  it('isRequired ignora espacios', () => {
    expect(isRequired('  ')).toBe(false)
    expect(isRequired('hola')).toBe(true)
  })

  it('isValidEmail', () => {
    expect(isValidEmail('dj@mgw.com')).toBe(true)
    expect(isValidEmail('dj@mgw')).toBe(false)
    expect(isValidEmail('')).toBe(false)
  })

  it('isValidBpm acepta enteros entre 40 y 220', () => {
    expect(isValidBpm('39')).toBe(false)
    expect(isValidBpm('40')).toBe(true)
    expect(isValidBpm('220')).toBe(true)
    expect(isValidBpm('300')).toBe(false)
    expect(isValidBpm('12.5')).toBe(false)
    expect(isValidBpm('')).toBe(false)
  })

  it('isValidHttpsUrl exige https', () => {
    expect(isValidHttpsUrl('https://soundcloud.com/x')).toBe(true)
    expect(isValidHttpsUrl('http://x.com')).toBe(false)
    expect(isValidHttpsUrl('https://')).toBe(false)
  })

  it('isValidPassword exige 8 caracteres o más', () => {
    expect(isValidPassword('1234567')).toBe(false)
    expect(isValidPassword('12345678')).toBe(true)
  })

  it('isValidCardNumber exige 16 dígitos y Luhn', () => {
    expect(isValidCardNumber('4111 1111 1111 1111')).toBe(true)
    expect(isValidCardNumber('4111 1111 1111 1112')).toBe(false)
    expect(isValidCardNumber('4111 1111 1111')).toBe(false)
  })

  it('isValidExpiry vale hasta el último día del mes', () => {
    expect(isValidExpiry('10/26', today)).toBe(true)
    expect(isValidExpiry('09/26', today)).toBe(false)
    expect(isValidExpiry('13/27', today)).toBe(false)
    expect(isValidExpiry('1/27', today)).toBe(false)
  })

  it('isValidCvv acepta 3 o 4 dígitos', () => {
    expect(isValidCvv('12')).toBe(false)
    expect(isValidCvv('123')).toBe(true)
    expect(isValidCvv('1234')).toBe(true)
    expect(isValidCvv('12a')).toBe(false)
  })

  it('maskCard deja solo los últimos 4', () => {
    expect(maskCard('4111 1111 1111 1111')).toBe('•••• 1111')
  })
})
