<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument;

/**
 * Interface TypeFactoryInterface
 */
interface TypeFactoryInterface
{
    /**
     * @param string $type
     * @return TypeConverterInterface
     */
    public function create($type);
}