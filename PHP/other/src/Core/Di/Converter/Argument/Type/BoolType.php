<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\Converter\Argument\TypeConverterInterface;

/**
 * Class BoolType
 */
class BoolType extends AbstractType implements TypeConverterInterface
{
    const TYPE_BOOL = 'bool';

    /**
     * @param array $data
     * @return bool
     */
    public function convert(array $data)
    {
        return $data[self::VALUE] === true || (int) $data[self::VALUE] === 1;
    }
}
